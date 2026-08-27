package com.skillport.server.web;

import com.skillport.server.domain.InstallTaskEntity;
import com.skillport.server.security.RequestUser;
import com.skillport.server.security.RequestUserFilter;
import com.skillport.server.service.InstallTaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/installations")
public class InstallController {
    private final InstallTaskService taskService;

    public InstallController(InstallTaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TaskResponse create(@RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
                               @Valid @RequestBody InstallRequest request) {
        return TaskResponse.from(taskService.create(user.userId(), request.skillId(), request.deviceId(), request.targets()));
    }

    @PostMapping("/uninstall")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TaskResponse uninstall(@RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
                                  @Valid @RequestBody InstallRequest request) {
        return TaskResponse.from(taskService.createUninstall(
                user.userId(), request.skillId(), request.deviceId(), request.targets()));
    }

    @GetMapping
    public TaskListResponse recent(@RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        return new TaskListResponse(taskService.recent(user.userId()).stream().map(TaskResponse::from).toList());
    }

    public record InstallRequest(@NotBlank String skillId, String deviceId, @NotEmpty List<String> targets) {
    }
    public record TaskListResponse(List<TaskResponse> tasks) {
    }
    public record TaskResponse(String id, String skillId, String deviceId, List<String> targets, String operation,
                               String status,
                               int progress, String stage, String errorMessage, Instant createdAt, Instant updatedAt) {
        static TaskResponse from(InstallTaskEntity task) {
            return new TaskResponse(task.getPublicId(), task.getSkillPublicId(), task.getDevicePublicId(),
                    List.of(task.getTargets().split(",")), task.getOperation(), task.getStatus(), task.getProgress(),
                    task.getStage(), task.getErrorMessage(), task.getCreatedAt(), task.getUpdatedAt());
        }
    }
}
