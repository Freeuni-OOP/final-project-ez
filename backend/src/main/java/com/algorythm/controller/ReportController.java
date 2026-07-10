package com.algorythm.controller;

import com.algorythm.dto.ReportRequest;
import com.algorythm.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reporting inappropriate content. Requires a signed-in user (under /api/**). */
@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/api/compositions/{id}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportComposition(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) ReportRequest request) {
        reportService.reportComposition(authentication.getName(), id, reasonOf(request));
    }

    @PostMapping("/api/comments/{id}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportComment(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) ReportRequest request) {
        reportService.reportComment(authentication.getName(), id, reasonOf(request));
    }

    private static String reasonOf(ReportRequest request) {
        return request == null ? null : request.reason();
    }
}
