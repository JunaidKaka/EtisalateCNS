package com.etisalat.services;

import com.etisalat.dto.CreateTemplateRequest;
import com.etisalat.models.ChangeSet;
import com.etisalat.models.Template;
import com.etisalat.models.TemplateHistory;
import com.etisalat.models.User;
import com.etisalat.repos.TemplateHistoryRepository;
import com.etisalat.repos.TemplateRepository;
import com.etisalat.utils.AuditUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class TemplateHistoryService {

    private final AuditUtils auditUtils;
    private final TemplateHistoryRepository historyRepository;
    private final TemplateRepository templateRepository;

    @Transactional
    public void updateTemplate(Long id, CreateTemplateRequest dto, User currentUser) {

        Template existing = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        Instant now = Instant.now();
        ChangeSet changeSet = new ChangeSet();

        updateField(changeSet, "code", existing.getCode(), dto.getCode(), existing::setCode);
        updateField(changeSet, "content", existing.getContent(), dto.getContent(), existing::setContent);
        updateField(changeSet, "title", existing.getTitle(), dto.getTitle(), existing::setTitle);
        updateField(changeSet, "subject", existing.getSubject(), dto.getSubject(), existing::setSubject);
        updateField(changeSet, "shortDesc", existing.getShortDesc(), dto.getShortDesc(), existing::setShortDesc);
        updateField(changeSet, "language", existing.getLanguage(), dto.getLanguage(), existing::setLanguage);
        updateField(changeSet, "senderId", existing.getSenderId(), dto.getSenderId(), existing::setSenderId);
        updateField(changeSet, "shortCode", existing.getShortCode(), dto.getShortCode(), existing::setShortCode);
        updateField(changeSet, "ppmId", existing.getPpmId(), dto.getPpmId(), existing::setPpmId);
        updateField(changeSet, "deploymentStatus", existing.getDeploymentStatus(), dto.getDeploymentStatus(), existing::setDeploymentStatus);
        updateField(changeSet, "category", existing.getCategory(), dto.getCategory(), existing::setCategory);

        if (changeSet.getCount() > 0) {
            saveHistory(existing, currentUser, now, changeSet);
        }

        existing.setModifiedBy(currentUser);
        existing.setModifiedAt(now);

        templateRepository.save(existing);
    }

    private <T> void updateField(
            ChangeSet changeSet,
            String fieldName,
            T oldValue,
            T newValue,
            Consumer<T> setter
    ) {
        if (newValue == null) return;

        String oldVal = normalize(oldValue);
        String newVal = normalize(newValue);

        if (!Objects.equals(oldVal, newVal)) {
            changeSet.addField(fieldName, oldVal, newVal);
            setter.accept(newValue);
        }
    }

    private void saveHistory(Template template, User user, Instant now, ChangeSet changeSet) {
        TemplateHistory history = new TemplateHistory();
        history.setTemplate(template);
        history.setChangedBy(user);
        history.setChangedAt(now);
        history.setChanges(changeSet);

        historyRepository.save(history);
    }

    private String normalize(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s.trim();
        return auditUtils.toJson(value);
    }
}
