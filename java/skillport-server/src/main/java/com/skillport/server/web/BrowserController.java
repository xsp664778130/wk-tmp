package com.skillport.server.web;

import com.skillport.server.config.SkillPortProperties;
import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.domain.SkillEntity;
import com.skillport.server.netty.BridgeSessionRegistry;
import com.skillport.server.security.RequestUser;
import com.skillport.server.security.RequestUserFilter;
import com.skillport.server.service.AuthService;
import com.skillport.server.service.DeviceService;
import com.skillport.server.service.DashboardStatisticsService;
import com.skillport.server.service.InstallTaskService;
import com.skillport.server.service.PairingService;
import com.skillport.server.service.PublicSkillService;
import com.skillport.server.service.SkillService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class BrowserController {
    private static final String SESSION_COOKIE_NAME = "skillport_session";

    private final AuthService authService;
    private final SkillService skillService;
    private final PublicSkillService publicSkillService;
    private final DeviceService deviceService;
    private final DashboardStatisticsService statisticsService;
    private final PairingService pairingService;
    private final InstallTaskService installTaskService;
    private final BridgeSessionRegistry sessionRegistry;
    private final SkillPortProperties properties;

    public BrowserController(AuthService authService, SkillService skillService,
                             PublicSkillService publicSkillService, DeviceService deviceService,
                             DashboardStatisticsService statisticsService,
                             PairingService pairingService, InstallTaskService installTaskService,
                             BridgeSessionRegistry sessionRegistry, SkillPortProperties properties) {
        this.authService = authService;
        this.skillService = skillService;
        this.publicSkillService = publicSkillService;
        this.deviceService = deviceService;
        this.statisticsService = statisticsService;
        this.pairingService = pairingService;
        this.installTaskService = installTaskService;
        this.sessionRegistry = sessionRegistry;
        this.properties = properties;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<BrowserAuthResponse> register(@Valid @RequestBody AuthController.RegisterRequest request,
                                                        HttpServletRequest servletRequest) {
        AuthService.SessionGrant grant = authService.register(request.email(), request.displayName(), request.password());
        return sessionResponse(grant, servletRequest, HttpStatus.CREATED);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<BrowserAuthResponse> login(@Valid @RequestBody AuthController.LoginRequest request,
                                                     HttpServletRequest servletRequest) {
        AuthService.SessionGrant grant = authService.login(request.email(), request.password());
        return sessionResponse(grant, servletRequest, HttpStatus.OK);
    }

    @GetMapping("/auth/me")
    public Map<String, AuthController.UserResponse> me(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        return Map.of("user", new AuthController.UserResponse(user.userId(), user.email(), user.displayName()));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(
            @CookieValue(name = SESSION_COOKIE_NAME, required = false, defaultValue = "") String token,
            HttpServletRequest request) {
        authService.logout(token);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredSessionCookie(request).toString())
                .build();
    }

    @GetMapping("/skills")
    public SkillController.SkillListResponse skills(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        Set<String> sharedSkillIds = publicSkillService.sharedSourceSkillIds(user.userId());
        return new SkillController.SkillListResponse(
                skillService.list(user.userId()).stream()
                        .map(skill -> SkillController.SkillResponse.from(
                                skill, sharedSkillIds.contains(skill.getPublicId())))
                        .toList());
    }

    @PostMapping(path = "/skills", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SkillController.SkillResponse upload(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @RequestParam String name,
            @RequestParam(defaultValue = "") String description,
            @RequestParam(defaultValue = "编程技能") String category,
            @RequestPart MultipartFile file) {
        return SkillController.SkillResponse.from(
                skillService.upload(user.userId(), name, description, category, file));
    }

    @PatchMapping("/skills")
    public SkillController.SkillResponse note(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @Valid @RequestBody BrowserNoteRequest request) {
        return SkillController.SkillResponse.from(
                skillService.updateNote(user.userId(), request.id(), request.note()));
    }

    @GetMapping("/public-skills")
    public PublicSkillController.PublicSkillListResponse publicSkills(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        return new PublicSkillController.PublicSkillListResponse(publicSkillService.list(user.userId()).stream()
                .map(PublicSkillController.PublicSkillResponse::from)
                .toList());
    }

    @PostMapping("/public-skills")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicSkillController.PublicSkillResponse sharePublicSkill(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @Valid @RequestBody PublicSkillController.ShareRequest request) {
        return PublicSkillController.PublicSkillResponse.from(publicSkillService.share(
                user.userId(), user.displayName(), request.skillId()), true);
    }

    @PostMapping("/public-skills/{publicSkillId}/pull")
    public PublicSkillController.PullResponse pullPublicSkill(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String publicSkillId) {
        PublicSkillService.PullResult result = publicSkillService.pull(user.userId(), publicSkillId);
        return new PublicSkillController.PullResponse(
                SkillController.SkillResponse.from(result.skill()), result.created());
    }

    @GetMapping("/skills/{skillId}/file")
    public ResponseEntity<InputStreamResource> content(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String skillId) throws IOException {
        SkillEntity skill = skillService.ownedSkill(user.userId(), skillId);
        Path file = skillService.ownedFile(user.userId(), skillId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(skill.getContentType()))
                .contentLength(skill.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(skill.getFileName()).build().toString())
                .header("X-Skill-Extension", SkillController.fileExtension(skill.getFileName()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(new InputStreamResource(Files.newInputStream(file)));
    }

    @GetMapping("/devices")
    public DeviceController.DeviceListResponse devices(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        List<DeviceController.DeviceResponse> devices = deviceService.list(user.userId()).stream()
                .map(device -> deviceResponse(device, sessionRegistry.isOnline(device.getPublicId())))
                .toList();
        return new DeviceController.DeviceListResponse(devices);
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatisticsService.DashboardStatistics> statistics(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(statisticsService.statistics(user.userId()));
    }

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceController.PairingCodeResponse pairingCode(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        PairingService.PairingCode code = pairingService.createCode(user.userId());
        return new DeviceController.PairingCodeResponse(
                code.code(), code.expiresAt(), properties.publicApiBaseUrl(), properties.publicNettyBaseUrl());
    }

    @GetMapping("/installs")
    public InstallController.TaskListResponse installs(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        return new InstallController.TaskListResponse(installTaskService.recent(user.userId()).stream()
                .map(InstallController.TaskResponse::from)
                .toList());
    }

    @PostMapping("/installs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public InstallController.TaskResponse install(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @Valid @RequestBody InstallController.InstallRequest request) {
        return InstallController.TaskResponse.from(installTaskService.create(
                user.userId(), request.skillId(), request.deviceId(), request.targets()));
    }

    private static DeviceController.DeviceResponse deviceResponse(DeviceEntity device, boolean online) {
        return new DeviceController.DeviceResponse(device.getPublicId(), device.getName(), device.getOs(),
                device.getArch(), online ? "ONLINE" : "OFFLINE", device.getLastSeenAt());
    }

    private static ResponseEntity<BrowserAuthResponse> sessionResponse(
            AuthService.SessionGrant grant, HttpServletRequest request, HttpStatus status) {
        Duration maxAge = Duration.between(Instant.now(), grant.expiresAt());
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, grant.token())
                .httpOnly(true)
                .secure(isSecure(request))
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
        BrowserAuthResponse body = new BrowserAuthResponse(AuthController.UserResponse.from(grant.user()));
        return ResponseEntity.status(status).header(HttpHeaders.SET_COOKIE, cookie.toString()).body(body);
    }

    private static ResponseCookie expiredSessionCookie(HttpServletRequest request) {
        return ResponseCookie.from(SESSION_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isSecure(request))
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private static boolean isSecure(HttpServletRequest request) {
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    public record BrowserAuthResponse(AuthController.UserResponse user) {
    }

    public record BrowserNoteRequest(@NotBlank String id, @Size(max = 2000) String note) {
    }
}
