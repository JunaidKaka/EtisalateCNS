// TemplateService.java
package com.etisalat.services;


import com.etisalat.dto.CreateTemplateRequest;
import com.etisalat.dto.TemplateUpdateDto;
import com.etisalat.models.*;
import com.etisalat.repos.GenericSpecifications;
import com.etisalat.repos.TemplateHistoryRepository;
import com.etisalat.repos.TemplateRepository;
import com.etisalat.repos.UserRepository;
import com.etisalat.utils.AuditUtils;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final TemplateHistoryService templateHistoryService;


    @Transactional
    public Template createTemplate(String username, CreateTemplateRequest req) {


        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        Template t = Template.builder()
                .type(req.getType())
                .content(req.getContent())
                .notid(req.getNotid())
                .code(req.getCode())
                .sname(req.getSname())
                .language(req.getLanguage())
                .senderId(req.getSenderId())
                .shortCode(req.getShortCode())
                .ppmId(req.getPpmId())
                .deploymentStatus(req.getDeploymentStatus())
                .title(req.getTitle())
                .shortDesc(req.getShortDesc())
                .linkUrl(req.getLinkUrl())
                .subject(  req.getType().equals(TemplateType.EMAIL) ? req.getSubject() : null)
                .category(req.getCategory())
                .createdBy(user)
                .createdAt(Instant.now())
                .status(TemplateStatus.PENDING_VERIFICATION)
                .editable(true)
                .build();

        return templateRepository.save(t);
    }

    public Page<Template> listTemplatesForUser(String username, String role, TemplateType type,
                                               TemplateStatus status, String q, String filtersJson, Pageable pageable) {

        List<String> allowedFields = List.of(
                "notid", "shortCode", "content","language", "status", "sname", "senderId", "title", "shortDesc", "createdBy.username",
                "changedAt"
        );

        List<String> globalFields = List.of("notid", "shortCode", "content", "title", "sname", "senderId");

        GenericSpecifications<Template> builder = new GenericSpecifications<>();
        Specification<Template> spec = Specification.where(
                builder.buildSpecification(
                        Template.class,
                        filtersJson,
                        q,
                        allowedFields,
                        globalFields
                )
        );


        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        final TemplateStatus s = status;
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), s));
        }

        switch (role) {
            case "DEV" -> {
                spec = spec.and((root, query, cb) ->
                        cb.or(
                                cb.equal(root.get("status"), TemplateStatus.PENDING_VERIFICATION),
                                cb.equal(root.get("status"), TemplateStatus.QA_REJECTED)
                        )
                );
            }
            case "QA" -> {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("status"), TemplateStatus.DEV_VERIFIED)
                );
            }
            case "OPERATION" -> {
                // OPERATION sees only approved
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("status"), TemplateStatus.QA_VERIFIED)
                );
            }
            case "ADMIN" -> {
                // no restriction
            }
            default -> {
                // fallback: no data
                spec = spec.and((root, query, cb) -> cb.disjunction());
            }
        }




        return templateRepository.findAll(spec, pageable);
    }

    public Optional<Template> getById(Long id) {
        return templateRepository.findById(id);
    }

    @Transactional
    public Template devVerify(Long id, String username) {
        Template t = templateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        // only allow verify if PENDING_VERIFICATION or QA_REJECTED
        if (!(t.getStatus() == TemplateStatus.PENDING_VERIFICATION || t.getStatus() == TemplateStatus.QA_REJECTED)) {
            throw new IllegalStateException("template not in a verifiable state");
        }


        t.setStatus(TemplateStatus.DEV_VERIFIED);
        t.setModifiedAt(Instant.now());
        t.setModifiedBy(t.getCreatedBy());
        t.setEditable(false);
        return templateRepository.save(t);
    }

    @Transactional
    public Template qaAccept(Long id, String qaUsername, String comments) {
        Template t = templateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        if (t.getStatus() != TemplateStatus.DEV_VERIFIED) {
            throw new IllegalStateException("only DEV_VERIFIED templates can be accepted by QA");
        }
        t.setStatus(TemplateStatus.QA_VERIFIED);
        t.setQaComments(comments);
        t.setModifiedAt(Instant.now());
        t.setEditable(false);
        // set modifiedBy to QA user
        userRepository.findByUsername(qaUsername).ifPresent(t::setModifiedBy);
        return templateRepository.save(t);
    }

    @Transactional
    public Template qaReject(Long id, String qaUsername, String comments) {
        Template t = templateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        if (t.getStatus() != TemplateStatus.DEV_VERIFIED) {
            throw new IllegalStateException("only DEV_VERIFIED templates can be rejected by QA");
        }
        t.setStatus(TemplateStatus.QA_REJECTED);
        t.setQaComments(comments);
        t.setModifiedAt(Instant.now());
        t.setEditable(true);
        userRepository.findByUsername(qaUsername).ifPresent(t::setModifiedBy);
        return templateRepository.save(t);
    }

    @Transactional
    public Template updateTemplate(Long id, String username, CreateTemplateRequest req) {
        Template t = templateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        if (!Boolean.TRUE.equals(t.getEditable())) {
            throw new IllegalStateException("template not editable in current status");
        }


        t.setContent(req.getContent());
        t.setTitle(req.getTitle());
        t.setShortDesc(req.getShortDesc());
        t.setLinkUrl(req.getLinkUrl());
        if(req.getType().equals(TemplateType.EMAIL)) {
        t.setSubject(req.getSubject());
        }
        t.setCategory(req.getCategory());
        t.setModifiedAt(Instant.now());
        t.setLanguage(req.getLanguage());
        t.setCode(req.getCode());
        t.setShortCode(req.getShortCode());
        t.setSname(req.getSname());
        Optional<User> user = userRepository.findByUsername(username);
        t.setModifiedBy(user.get());

        CompletableFuture.runAsync(() -> templateHistoryService.updateTemplate(id, req, user.get())) ;
        return templateRepository.save(t);
    }

    @Transactional(readOnly = true)
    public Integer getNextNotId(String sname) {

        Integer max = templateRepository.findMaxNotIdBySystem(sname);

        if (max == null) {
            return 1;
        }

        return max + 1;
    }

    public List<String> getDistinctSystemNames() {
        return templateRepository.findDistinctSystemNames();
    }


}
