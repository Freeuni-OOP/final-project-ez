package com.algorythm.controller;

import com.algorythm.dto.ReportResponse;
import com.algorythm.dto.SiteStatsResponse;
import com.algorythm.service.AdminService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only moderation. Lives under /api/admin/** (authenticated like the rest of
 * /api); the service refuses anyone who isn't an admin with 403.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/reports")
    public List<ReportResponse> reports(Authentication authentication) {
        return adminService.listOpenReports(authentication.getName());
    }

    @DeleteMapping("/compositions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeComposition(Authentication authentication, @PathVariable Long id) {
        adminService.removeComposition(authentication.getName(), id);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeComment(Authentication authentication, @PathVariable Long id) {
        adminService.removeComment(authentication.getName(), id);
    }

    @PostMapping("/reports/{id}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismissReport(Authentication authentication, @PathVariable Long id) {
        adminService.dismissReport(authentication.getName(), id);
    }

    @PostMapping("/users/{username}/promote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promote(Authentication authentication, @PathVariable String username) {
        adminService.promote(authentication.getName(), username);
    }

    @GetMapping("/stats")
    public SiteStatsResponse stats(Authentication authentication) {
        return adminService.stats(authentication.getName());
    }
}
