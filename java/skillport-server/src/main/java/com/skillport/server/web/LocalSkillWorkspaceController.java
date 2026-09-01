package com.skillport.server.web;

import com.skillport.server.security.RequestUser;
import com.skillport.server.security.RequestUserFilter;
import com.skillport.server.service.InstallTaskService;
import com.skillport.server.service.LocalSkillWorkspaceService;
import com.skillport.server.service.LocalSkillRemoteAccessService;
import com.skillport.protocol.LocalSkillActionResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices/{deviceId}/local-skills")
public class LocalSkillWorkspaceController {
    private final LocalSkillWorkspaceService workspaceService;
    private final InstallTaskService installTaskService;
    private final LocalSkillRemoteAccessService remoteAccessService;

    public LocalSkillWorkspaceController(LocalSkillWorkspaceService workspaceService,
                                         InstallTaskService installTaskService,
                                         LocalSkillRemoteAccessService remoteAccessService) {
        this.workspaceService = workspaceService;
        this.installTaskService = installTaskService;
        this.remoteAccessService = remoteAccessService;
    }

    @GetMapping
    public LocalSkillWorkspaceService.WorkspaceView workspace(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String deviceId) {
        return workspaceService.workspace(user.userId(), deviceId);
    }

    @PostMapping("/uninstall")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public InstallController.TaskResponse uninstall(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String deviceId,
            @Valid @RequestBody LocalUninstallRequest request) {
        return InstallController.TaskResponse.from(installTaskService.createLocalUninstall(
                user.userId(), deviceId, request.tool(), request.slug()));
    }

    @PostMapping("/open-folder")
    public LocalSkillActionResult openFolder(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String deviceId,
            @Valid @RequestBody LocalUninstallRequest request) {
        return remoteAccessService.openFolder(user.userId(), deviceId, request.tool(), request.slug());
    }

    @PostMapping("/manifest")
    public LocalSkillActionResult manifest(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String deviceId,
            @Valid @RequestBody LocalUninstallRequest request) {
        return remoteAccessService.readManifest(user.userId(), deviceId, request.tool(), request.slug());
    }

    @PostMapping("/environment/read")
    public LocalSkillActionResult environment(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String deviceId,
            @Valid @RequestBody LocalUninstallRequest request) {
        return remoteAccessService.readEnvironment(user.userId(), deviceId, request.tool(), request.slug());
    }

    @PatchMapping("/environment")
    public LocalSkillActionResult updateEnvironment(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String deviceId,
            @Valid @RequestBody LocalEnvironmentUpdateRequest request) {
        return remoteAccessService.updateEnvironment(user.userId(), deviceId,
                request.tool(), request.slug(), request.values());
    }

    public record LocalUninstallRequest(@NotBlank @Size(max = 32) String tool,
                                        @NotBlank @Size(max = 180) String slug) {
    }

    public record LocalEnvironmentUpdateRequest(@NotBlank @Size(max = 32) String tool,
                                                @NotBlank @Size(max = 180) String slug,
                                                @NotNull @Size(min = 1, max = 200) Map<String, String> values) {
    }
}
