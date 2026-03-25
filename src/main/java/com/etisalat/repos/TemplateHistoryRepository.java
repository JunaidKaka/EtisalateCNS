package com.etisalat.repos;

import com.etisalat.models.TemplateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TemplateHistoryRepository extends JpaRepository<TemplateHistory, Long> , JpaSpecificationExecutor<TemplateHistory> {
    List<TemplateHistory> findByTemplateIdOrderByChangedAtDesc(Long templateId);
}