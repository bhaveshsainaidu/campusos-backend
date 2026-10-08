package com.campusos.notice;

import com.campusos.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * Notices visible to a viewer, newest first, paginated.
     * Admin sees everything; everyone sees ALL + notices targeted to their role/department/batch.
     */
    @Query(value = """
        SELECT n FROM Notice n
        LEFT JOIN FETCH n.createdBy u
        LEFT JOIN FETCH n.department d
        LEFT JOIN FETCH n.batch b
        WHERE (:admin = true)
           OR (n.audience = com.campusos.notice.Notice.Audience.ALL)
           OR (n.audience = com.campusos.notice.Notice.Audience.ROLE AND n.targetRole = :role)
           OR (n.audience = com.campusos.notice.Notice.Audience.DEPARTMENT AND n.department.id = :departmentId)
           OR (n.audience = com.campusos.notice.Notice.Audience.BATCH AND n.batch.id = :batchId)
        """,
        countQuery = """
        SELECT count(n) FROM Notice n
        WHERE (:admin = true)
           OR (n.audience = com.campusos.notice.Notice.Audience.ALL)
           OR (n.audience = com.campusos.notice.Notice.Audience.ROLE AND n.targetRole = :role)
           OR (n.audience = com.campusos.notice.Notice.Audience.DEPARTMENT AND n.department.id = :departmentId)
           OR (n.audience = com.campusos.notice.Notice.Audience.BATCH AND n.batch.id = :batchId)
        """)
    Page<Notice> visibleFor(@Param("admin") boolean admin,
                            @Param("role") Role role,
                            @Param("departmentId") Long departmentId,
                            @Param("batchId") Long batchId,
                            Pageable pageable);
}
