package com.algorythm.service;

import com.algorythm.model.Comment;
import com.algorythm.model.Composition;
import com.algorythm.model.Report;
import com.algorythm.model.ReportStatus;
import com.algorythm.model.User;
import com.algorythm.repository.CommentRepository;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.ReportRepository;
import com.algorythm.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Lets a signed-in user report a composition or a comment as inappropriate. */
@Service
public class ReportService {

    private final ReportRepository reports;
    private final CompositionRepository compositions;
    private final CommentRepository comments;
    private final UserRepository users;

    public ReportService(ReportRepository reports,
                         CompositionRepository compositions,
                         CommentRepository comments,
                         UserRepository users) {
        this.reports = reports;
        this.compositions = compositions;
        this.comments = comments;
        this.users = users;
    }

    /** Report a public composition. A second open report from the same user is ignored. */
    @Transactional
    public void reportComposition(String username, Long compositionId, String reason) {
        User reporter = currentUser(username);
        Composition composition = compositions.findById(compositionId)
                .filter(Composition::isPublic)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Composition not found"));
        if (!reports.existsByReporterAndCompositionAndStatus(
                reporter, composition, ReportStatus.OPEN)) {
            reports.save(Report.forComposition(reporter, composition, reason));
        }
    }

    /** Report a comment. A second open report from the same user is ignored. */
    @Transactional
    public void reportComment(String username, Long commentId, String reason) {
        User reporter = currentUser(username);
        Comment comment = comments.findById(commentId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!reports.existsByReporterAndCommentAndStatus(reporter, comment, ReportStatus.OPEN)) {
            reports.save(Report.forComment(reporter, comment, reason));
        }
    }

    private User currentUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
