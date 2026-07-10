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
 * Admin-only moderation. Every method first confirms the caller is an admin, so a
 * normal user who reaches these endpoints is refused with 403.
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
    public List<ReportResponse> listOpenReports(String adminUsername) {
        requireAdmin(adminUsername);
        return reports.findByStatusOrderByCreatedAtDesc(ReportStatus.OPEN).stream()
                .map(ReportResponse::from)
                .toList();
    }

    /** Remove any composition. Its reports and dependent rows cascade away. */
    @Transactional
    public void removeComposition(String adminUsername, Long compositionId) {
        requireAdmin(adminUsername);
        Composition composition = compositions.findById(compositionId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Composition not found"));
        compositions.delete(composition);
    }

    /** Remove any comment. */
    @Transactional
    public void removeComment(String adminUsername, Long commentId) {
        requireAdmin(adminUsername);
        Comment comment = comments.findById(commentId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        comments.delete(comment);
    }

    /** Mark a report resolved without removing the content (it was fine). */
    @Transactional
    public void dismissReport(String adminUsername, Long reportId) {
        requireAdmin(adminUsername);
        Report report = reports.findById(reportId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        report.setStatus(ReportStatus.RESOLVED);
    }

    /** Promote another user to admin. */
    @Transactional
    public void promote(String adminUsername, String targetUsername) {
        requireAdmin(adminUsername);
        User target = users.findByUsername(targetUsername)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        target.setRole(Role.ADMIN);
    }

    /** Basic site numbers. */
    @Transactional(readOnly = true)
    public SiteStatsResponse stats(String adminUsername) {
        requireAdmin(adminUsername);
        return new SiteStatsResponse(
                users.count(),
                compositions.count(),
                reports.countByStatus(ReportStatus.OPEN));
    }

    private User requireAdmin(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins only");
        }
        return user;
    }
}
