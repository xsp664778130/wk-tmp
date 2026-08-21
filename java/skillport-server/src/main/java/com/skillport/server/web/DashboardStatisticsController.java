package com.skillport.server.web;

import com.skillport.server.security.RequestUser;
import com.skillport.server.security.RequestUserFilter;
import com.skillport.server.service.DashboardStatisticsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class DashboardStatisticsController {
    private final DashboardStatisticsService statisticsService;

    public DashboardStatisticsController(DashboardStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public ResponseEntity<DashboardStatisticsService.DashboardStatistics> statistics(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(statisticsService.statistics(user.userId()));
    }
}
