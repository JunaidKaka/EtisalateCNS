package com.etisalat.models;

import com.etisalat.convertor.ChangeSetConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "template_history",
       indexes = {
           @Index(name = "idx_template_history_template", columnList = "template_id"),
           @Index(name = "idx_template_history_changed_at", columnList = "changed_at")
       })
@Setter
@Getter
public class TemplateHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;


    @Lob
    @Column(name = "changes", columnDefinition = "TEXT")
    @Convert(converter = ChangeSetConverter.class)
    private ChangeSet changes;

    @Column(name = "comment", length = 2000)
    private String comment;


}