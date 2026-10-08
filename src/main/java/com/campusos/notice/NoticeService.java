package com.campusos.notice;

import com.campusos.academics.BatchRepository;
import com.campusos.academics.DepartmentRepository;
import com.campusos.common.ApiException;
import com.campusos.common.PageResponse;
import com.campusos.notice.dto.NoticeDtos.*;
import com.campusos.security.AuthPrincipal;
import com.campusos.student.Student;
import com.campusos.student.StudentRepository;
import com.campusos.user.Role;
import com.campusos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final BatchRepository batchRepository;
    private final StudentRepository studentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public PageResponse<NoticeResponse> visibleNotices(AuthPrincipal viewer, int page, int size) {
        boolean admin = "ADMIN".equals(viewer.role());
        Long departmentId = null;
        Long batchId = null;
        if ("STUDENT".equals(viewer.role())) {
            var student = studentRepository.findByUserId(viewer.id()).orElse(null);
            if (student != null) {
                departmentId = student.getDepartment() != null ? student.getDepartment().getId() : null;
                batchId = student.getBatch() != null ? student.getBatch().getId() : null;
            }
        }
        Role role = Role.valueOf(viewer.role());
        Page<Notice> p = noticeRepository.visibleFor(admin, role, departmentId, batchId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(p, NoticeService::toResponse);
    }

    /** Creates the notice and pushes it over WebSocket to the targeted topic. */
    @Transactional
    public NoticeResponse create(AuthPrincipal principal, CreateNoticeRequest req) {
        if (req.audience() == Notice.Audience.ROLE && req.targetRole() == null) {
            throw ApiException.badRequest("targetRole is required for ROLE audience");
        }
        if (req.audience() == Notice.Audience.DEPARTMENT && req.departmentId() == null) {
            throw ApiException.badRequest("departmentId is required for DEPARTMENT audience");
        }
        if (req.audience() == Notice.Audience.BATCH && req.batchId() == null) {
            throw ApiException.badRequest("batchId is required for BATCH audience");
        }

        Notice notice = Notice.builder()
                .title(req.title())
                .body(req.body())
                .audience(req.audience())
                .targetRole(req.targetRole())
                .department(req.departmentId() != null ? departmentRepository.getReferenceById(req.departmentId()) : null)
                .batch(req.batchId() != null ? batchRepository.getReferenceById(req.batchId()) : null)
                .createdBy(userRepository.getReferenceById(principal.id()))
                .build();
        notice = noticeRepository.save(notice);

        NoticeResponse response = toResponse(notice);
        String topic = topicFor(response);
        messagingTemplate.convertAndSend(topic, Map.of(
                "type", "NOTICE_CREATED",
                "notice", response));
        log.info("Notice {} pushed to topic {}", notice.getId(), topic);
        return response;
    }

    static String topicFor(NoticeResponse n) {
        return switch (n.audience()) {
            case ROLE -> "/topic/notices/role/" + n.targetRole();
            case DEPARTMENT -> "/topic/notices/department/" + n.departmentId();
            case BATCH -> "/topic/notices/batch/" + n.batchId();
            case ALL -> "/topic/notices/all";
        };
    }

    static NoticeResponse toResponse(Notice n) {
        return new NoticeResponse(n.getId(), n.getTitle(), n.getBody(), n.getAudience(),
                n.getTargetRole() != null ? n.getTargetRole().name() : null,
                n.getDepartment() != null ? n.getDepartment().getId() : null,
                n.getDepartment() != null ? n.getDepartment().getCode() : null,
                n.getBatch() != null ? n.getBatch().getId() : null,
                n.getBatch() != null ? n.getBatch().getName() : null,
                n.getCreatedBy().getId(), n.getCreatedBy().getFullName(), n.getCreatedAt());
    }
}
