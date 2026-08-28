package com.skillport.server.web;

import com.skillport.server.security.RequestUser;
import com.skillport.server.security.RequestUserFilter;
import com.skillport.server.service.InstallTaskService;
import com.skillport.server.service.LocalSkillWorkspaceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices/{deviceId}/local-skills")
public class LocalSkillWorkspaceController {
    private final LocalSkillWorkspaceService workspaceService;
    private final InstallTaskService installTaskService;

    public LocalSkillWorkspaceController(LocalSkillWorkspaceService workspaceService,
                                         InstallTaskService installTaskService) {
        this.workspaceService = workspaceService;
        this.installTaskService = installTaskService;
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

    public record LocalUninstallRequest(@NotBlank @Size(max = 32) String tool,
                                        @NotBlank @Size(max = 180) String slug) {
    }
}
