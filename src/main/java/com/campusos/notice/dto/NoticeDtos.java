package com.campusos.notice.dto;

import com.campusos.notice.Notice;
import com.campusos.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class NoticeDtos {
    private NoticeDtos() {}

    public record CreateNoticeRequest(
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 5000) String body,
            @NotNull Notice.Audience audience,
            Role targetRole,
            Long departmentId,
            Long batchId) {}

    public record NoticeResponse(Long id, String title, String body, Notice.Audience audience,
                                 String targetRole, Long departmentId, String departmentCode,
                                 Long batchId, String batchName,
                                 Long createdById, String createdByName, Instant createdAt) {}
}
