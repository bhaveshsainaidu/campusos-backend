package com.campusos.notice;

import com.campusos.common.PageResponse;
import com.campusos.notice.dto.NoticeDtos.*;
import com.campusos.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
@Tag(name = "Notices")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    @Operation(summary = "Notices visible to the current user (paginated, newest first)")
    public PageResponse<NoticeResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return noticeService.visibleNotices(principal, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Publish a notice (ALL / ROLE / DEPARTMENT / BATCH). Pushes over WebSocket.")
    public NoticeResponse create(@Valid @RequestBody CreateNoticeRequest request,
                                 @AuthenticationPrincipal AuthPrincipal principal) {
        return noticeService.create(principal, request);
    }
}
