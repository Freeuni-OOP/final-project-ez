package com.algorythm.service;

import com.algorythm.dto.ReportResponse;
import com.algorythm.dto.SiteStatsResponse;
import com.algorythm.model.Comment;
import com.algorythm.model.Composition;
import com.algorythm.model.Report;
import com.algorythm.model.ReportStatus;
import com.algorythm.model.Role;
import com.algorythm.model.User;
import com.algorythm.repository.CommentRepository;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.ReportRepository;
import com.algorythm.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin-only moderation. Access is enforced centrally by the security config
 * (hasRole("ADMIN") on /api/admin/**), so these methods no longer re-check the
 * caller's role — a non-admin never reaches them.
 */
@Service
public class AdminService {

    private final ReportRepository reports;
    private final CompositionRepository compositions;
    private final CommentRepository comments;
    private final UserRepository users;

    public AdminService(ReportRepository reports,
                        CompositionRepository compositions,
                        CommentRepository comments,
                        UserRepository users) {
        this.reports = reports;
        this.compositions = compositions;
        this.comments = comments;
        this.users = users;
    }

    /** Open reports, newest first. */
    @Transactional(readOnly = true)
    public List<ReportResponse> listOpenReports() {
        return reports.findByStatusOrderByCreatedAtDesc(ReportStatus.OPEN).stream()
                .map(ReportResponse::from)
                .toList();
    }

    /** Remove any composition. Its reports and dependent rows cascade away. */
    @Transactional
    public void removeComposition(Long compositionId) {
        Composition composition = compositions.findById(compositionId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Composition not found"));
        compositions.delete(composition);
    }

    /** Remove any comment. */
    @Transactional
    public void removeComment(Long commentId) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        comments.delete(comment);
    }

    /** Mark a report resolved without removing the content (it was fine). */
    @Transactional
    public void dismissReport(Long reportId) {
        Report report = reports.findById(reportId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        report.setStatus(ReportStatus.RESOLVED);
    }

    /** Promote another user to admin. */
    @Transactional
    public void promote(String targetUsername) {
        User target = users.findByUsername(targetUsername)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        target.setRole(Role.ADMIN);
    }

    /** Basic site numbers. */
    @Transactional(readOnly = true)
    public SiteStatsResponse stats() {
        return new SiteStatsResponse(
                users.count(),
                compositions.count(),
                reports.countByStatus(ReportStatus.OPEN));
    }
}
