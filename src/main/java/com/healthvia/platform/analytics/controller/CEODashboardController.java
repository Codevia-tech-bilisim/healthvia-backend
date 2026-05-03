package com.healthvia.platform.analytics.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.analytics.dto.CEODashboardDtos;
import com.healthvia.platform.analytics.service.CEODashboardService;
import com.healthvia.platform.common.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analytics/ceo")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERADMIN', 'CEO', 'ADMIN')")
public class CEODashboardController {

    private final CEODashboardService service;

    @GetMapping("/overview")
    public ApiResponse<CEODashboardDtos.Overview> overview(
            @RequestParam(defaultValue = "month") String range) {
        return ApiResponse.success(service.getOverview(range));
    }

    @GetMapping("/revenue-timeseries")
    public ApiResponse<List<CEODashboardDtos.RevenueTimeseriesPoint>> revenueTimeseries(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(defaultValue = "day") String granularity) {
        return ApiResponse.success(service.getRevenueTimeseries(range, granularity));
    }

    @GetMapping("/pipeline-funnel")
    public ApiResponse<List<CEODashboardDtos.PipelineFunnelPoint>> funnel(
            @RequestParam(defaultValue = "month") String range) {
        return ApiResponse.success(service.getFunnel(range));
    }

    @GetMapping("/agent-performance")
    public ApiResponse<List<CEODashboardDtos.AgentPerformance>> agents(
            @RequestParam(defaultValue = "month") String range) {
        return ApiResponse.success(service.getAgentLeaderboard(range));
    }

    @GetMapping("/channel-breakdown")
    public ApiResponse<List<CEODashboardDtos.ChannelBreakdownPoint>> channels(
            @RequestParam(defaultValue = "month") String range) {
        return ApiResponse.success(service.getChannelBreakdown(range));
    }

    @GetMapping("/treatment-demand")
    public ApiResponse<List<CEODashboardDtos.TreatmentDemandPoint>> treatments(
            @RequestParam(defaultValue = "month") String range) {
        return ApiResponse.success(service.getTreatmentDemand(range));
    }

    @GetMapping("/geo-distribution")
    public ApiResponse<List<CEODashboardDtos.GeoDistributionPoint>> geo(
            @RequestParam(defaultValue = "month") String range) {
        return ApiResponse.success(service.getGeoDistribution(range));
    }

    @GetMapping("/sla-metrics")
    public ApiResponse<Object> slaMetrics(@RequestParam(defaultValue = "month") String range) {
        // Placeholder — future expansion; keeps the full endpoint surface covered.
        return ApiResponse.success(service.getOverview(range).getOperations());
    }
}
