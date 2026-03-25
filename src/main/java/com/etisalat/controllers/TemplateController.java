// TemplateController.java
package com.etisalat.controllers;

import com.etisalat.dto.*;
import com.etisalat.models.Template;
import com.etisalat.models.TemplateStatus;
import com.etisalat.models.TemplateType;
import com.etisalat.repos.UserRepository;
import com.etisalat.services.TemplateService;

import com.etisalat.utils.ResponseBuilder;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    private String getUsername(Authentication auth) {
        return auth == null ? null : auth.getName();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TemplateResponse>> create(
             @RequestBody CreateTemplateRequest req,
            Authentication auth) {

        String username = getUsername(auth);
        Template t = templateService.createTemplate(username, req);

        return ResponseEntity.ok(
                ResponseBuilder.success(
                        toResponse(t),
                        "Template created successfully. Status: PENDING_VERIFICATION"
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEV')")
    public ResponseEntity<ApiResponse<TemplateResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateTemplateRequest req,
            Authentication auth) {

        String username = getUsername(auth);
        Template t = templateService.updateTemplate(id, username, req);

        return ResponseEntity.ok(
                ResponseBuilder.success(
                        toResponse(t),
                        "Template updated successfully"
                )
        );
    }

    @PostMapping("/{id}/verify")
//    @PreAuthorize("hasAnyAuthority('DEV')")
    public ResponseEntity<ApiResponse<TemplateResponse>> devVerify(
            @PathVariable Long id,
            Authentication auth) {

        String username = getUsername(auth);
        Template t = templateService.devVerify(id, username);

        return ResponseEntity.ok(
                ResponseBuilder.success(
                        toResponse(t),
                        "Template verified by Developer"
                )
        );
    }

    @PostMapping("/{id}/accept")
//    @PreAuthorize("hasAnyAuthority('QA')")
    public ResponseEntity<ApiResponse<TemplateResponse>> qaAccept(
            @PathVariable Long id,
            @RequestBody(required = false) QAActionRequest req,
            Authentication auth) {

        String username = getUsername(auth);
        Template t = templateService.qaAccept(
                id,
                username,
                req == null ? null : req.getComments()
        );

        return ResponseEntity.ok(
                ResponseBuilder.success(
                        toResponse(t),
                        "Template approved by QA"
                )
        );
    }


    @PostMapping("/{id}/reject")
//    @PreAuthorize("hasAnyAuthority('QA')")
    public ResponseEntity<ApiResponse<TemplateResponse>> qaReject(
            @PathVariable Long id,
            @RequestBody(required = false) QAActionRequest req,
            Authentication auth) {

        String username = getUsername(auth);
        Template t = templateService.qaReject(
                id,
                username,
                req == null ? null : req.getComments()
        );

        return ResponseEntity.ok(
                ResponseBuilder.success(
                        toResponse(t),
                        "Template rejected by QA and returned to Developer"
                )
        );
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEV','QA','OPERATION')")
    public ResponseEntity<ApiResponse<TemplateResponse>> getOne(
            @PathVariable Long id) {

        Template t = templateService.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        return ResponseEntity.ok(
                ResponseBuilder.success(
                        toResponse(t),
                        "Template fetched successfully"
                )
        );
    }


    @GetMapping
    @PreAuthorize("hasAnyAuthority('DEV','QA','OPERATION')")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> list(
            @RequestParam(required = false) TemplateType type,
            @RequestParam(required = false) TemplateStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filters,
            Pageable pageable,
            Authentication auth) {

        String username = getUsername(auth);
        String role = auth.getAuthorities()
                .stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("");

        Page<Template> page = templateService.listTemplatesForUser(username, role, type, status, q, filters, pageable);

        List<TemplateResponse> data =
                page.getContent().stream()
                        .map(this::toResponse)
                        .toList();

        Meta meta = new Meta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(
                ResponseBuilder.withMeta(
                        data,
                        meta,
                        "Templates fetched successfully"
                )
        );
    }

    @GetMapping("/systems")
    public ResponseEntity<ApiResponse> getDistinctSystems() {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        templateService.getDistinctSystemNames(),
                        "system names  fetched successfully"
                )
        );
    }

    @GetMapping("/next-notid")
    public ResponseEntity<ApiResponse> getNextNotId(@RequestParam String sname) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        templateService.getNextNotId(sname),
                        "Next notification id for "+ sname +" fetched successfully"
                )
        );

    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('DEV','QA','OPERATION')")
    public ResponseEntity<byte[]> downloadHtml(@PathVariable Long id) {
        Template t = templateService.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        String filename = safeFileName(t.getTitle(), id) + ".html";
        byte[] bytes = t.getContent() == null ? new byte[0] : t.getContent().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setContentLength(bytes.length);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(filename));

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private static String safeFileName(String title, Long id) {
        String base = (title == null || title.isBlank()) ? "template-" + id : title.trim();
        return base.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "-").replaceAll("[^\\p{Alnum}\\-_.]", "");
    }

    private static String buildContentDisposition(String filename) {
        String basic = "attachment; filename=\"" + filename.replace("\"", "") + "\"";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20"); // spaces as %20
        String rfc5987 = "filename*=UTF-8''" + encoded;
        return basic + "; " + rfc5987;
    }


    private TemplateResponse toResponse(Template t) {
        return TemplateResponse.builder()
                .id(t.getId())
                .senderId(t.getSenderId())
                .shortcode(t.getShortCode())
                .deploymentstatus(t.getDeploymentStatus())
                .ppmID(t.getPpmId())
                .notid(t.getNotid())
                .type(t.getType())
                .content(t.getContent())
                .title(t.getTitle())
                .subject(t.getSubject())
                .category(t.getCategory())
                .status(t.getStatus())
                .createdBy(t.getCreatedBy() == null ? null : t.getCreatedBy().getUsername())
                .createdAt(t.getCreatedAt())
                .language(t.getLanguage())
                .qaComments(t.getQaComments())
                .systemName(t.getSname())
                .build();
    }
}
