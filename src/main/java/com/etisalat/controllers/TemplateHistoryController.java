package com.etisalat.controllers;

import com.etisalat.dto.ApiResponse;
import com.etisalat.dto.Meta;
import com.etisalat.models.TemplateHistory;
import com.etisalat.services.TemplateHistoryQueryService;
import com.etisalat.utils.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TemplateHistoryController {

    private final TemplateHistoryQueryService queryService;

    /**
     * GET /api/templates/{templateId}/history
     * Query params:
     *   - page (0-based)
     *   - size
     *   - sort (e.g. changedAt,desc)
     *   - filters (stringified JSON from PrimeNG)  -> param name "filters"
     *   - q (global search)
     */
    @GetMapping("/templates/{templateId}/history")
    public ResponseEntity<ApiResponse> getByTemplate(
            @PathVariable Long templateId,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        // whitelist fields client is allowed to filter on
        List<String> allowedFields = List.of("changedAt", "changedBy.username", "notiId");
        List<String> globalFields = List.of("changes", "changedBy.username", "notiId");

        Page<TemplateHistory> pageR =    queryService.queryByTemplateId(templateId, filters, q, allowedFields, globalFields, pageable);


        Meta meta = new Meta(
                pageR.getNumber(),
                pageR.getSize(),
                pageR.getTotalElements(),
                pageR.getTotalPages()
        );

        return ResponseEntity.ok(
                ResponseBuilder.withMeta(
                        pageR.getContent(),
                        meta,
                        "Templates fetched successfully"
                )
        );

    }

    /**
     * GET /api/history  -- returns all history with filtering/paging
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse> getAllHistory(
            @RequestParam(required = false) String filters,
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        List<String> allowedFields = List.of("template.notid","template.id", "changedAt", "changedBy.username", "notiId");
        List<String> globalFields = List.of("changes", "changedBy.username", "template.notid");

        Page<TemplateHistory> pageR =   queryService.queryAll(filters, q, allowedFields, globalFields, pageable);
        Meta meta = new Meta(
                pageR.getNumber(),
                pageR.getSize(),
                pageR.getTotalElements(),
                pageR.getTotalPages()
        );

        return ResponseEntity.ok(
                ResponseBuilder.withMeta(
                        pageR.getContent(),
                        meta,
                        "Templates fetched successfully"
                )
        );
    }

    private Pageable toPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "changedAt"));
        }

        String[] pieces = sort.split(",");
        if (pieces.length == 2) {
            Sort.Direction dir = pieces[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, size, Sort.by(dir, pieces[0]));
        } else {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "changedAt"));
        }
    }
}