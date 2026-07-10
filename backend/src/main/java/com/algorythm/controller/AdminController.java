package com.algorythm.controller;

import com.algorythm.dto.ReportResponse;
import com.algorythm.dto.SiteStatsResponse;
import com.algorythm.service.AdminService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only moderation under /api/admin/**. The security config restricts this
 * whole path to hasRole("ADMIN"), so a non-admin is refused with 403 before any
 * of these methods run.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/reports")
    public List<ReportResponse> reports() {
        return adminService.listOpenReports();
    }

    @DeleteMapping("/compositions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeComposition(@PathVariable Long id) {
        adminService.removeComposition(id);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeComment(@PathVariable Long id) {
        adminService.removeComment(id);
    }

    @PostMapping("/reports/{id}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismissReport(@PathVariable Long id) {
        adminService.dismissReport(id);
    }

    @PostMapping("/users/{username}/promote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promote(@PathVariable String username) {
        adminService.promote(username);
    }

    @GetMapping("/stats")
    public SiteStatsResponse stats() {
        return adminService.stats();
    }
}
