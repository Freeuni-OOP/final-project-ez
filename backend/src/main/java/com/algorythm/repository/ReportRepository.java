package com.algorythm.repository;

import com.algorythm.model.Comment;
import com.algorythm.model.Composition;
import com.algorythm.model.Report;
import com.algorythm.model.ReportStatus;
import com.algorythm.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for reports: the admin review list, counts, and duplicate checks. */
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    long countByStatus(ReportStatus status);

    boolean existsByReporterAndCompositionAndStatus(
            User reporter, Composition composition, ReportStatus status);

    boolean existsByReporterAndCommentAndStatus(
            User reporter, Comment comment, ReportStatus status);
}
