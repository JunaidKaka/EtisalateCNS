package com.etisalat.services;

import com.etisalat.models.Template;
import com.etisalat.models.TemplateHistory;
import com.etisalat.repos.GenericSpecifications;
import com.etisalat.repos.TemplateHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TemplateHistoryQueryService {

    private final TemplateHistoryRepository historyRepository;

    private final ObjectMapper objectMapper; // for parsing 'changes' JSON

    /**
     * Generic "all history" with filters/sorting/paging
     * Accepts:
     *  - filtersJson : PrimeNG-style filters JSON (string)
     *  - q : global search string
     *  - allowedFields / globalFields : provide lists to avoid searching arbitrary fields
     */
    public Page<TemplateHistory> queryAll(String filtersJson,
                                             String q,
                                             List<String> allowedFields,
                                             List<String> globalFields,
                                             org.springframework.data.domain.Pageable pageable) {


        GenericSpecifications<TemplateHistory> specsBuilder = new GenericSpecifications<>();
        Specification<TemplateHistory> spec = specsBuilder.buildSpecification(
                TemplateHistory.class,
                filtersJson,
                q,
                allowedFields,
                globalFields
        );

        Page<TemplateHistory> page = historyRepository.findAll(spec, pageable);
        return page;
    }

    /**
     * Template-specific history
     */
    public Page<TemplateHistory> queryByTemplateId(Long templateId,
                                                      String filtersJson,
                                                      String q,
                                                      List<String> allowedFields,
                                                      List<String> globalFields,
                                                      Pageable pageable) {
        GenericSpecifications<TemplateHistory> specsBuilder = new GenericSpecifications<>();

        Specification<TemplateHistory> baseSpec = specsBuilder.buildSpecification(
                TemplateHistory.class,
                filtersJson,
                q,
                allowedFields,
                globalFields
        );


        Specification<TemplateHistory> templateSpec = (root, query, cb) ->
                cb.equal(root.get("template").get("id"), templateId);

        Specification<TemplateHistory> finalSpec = Specification.where(templateSpec).and(baseSpec);
        Page<TemplateHistory> page = historyRepository.findAll(finalSpec, pageable);
        return page;
    }

}