// Template.java
package com.etisalat.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateType type;

    @Column(name="notid", length = 128)
    private long notid;

    @Column(name="code", length = 128)
    private String code;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name="sname", length = 128)
    private String sname;

    @Column(name="language", length = 32)
    private String language;

    @Column(name="sender_id", length = 64)
    private String senderId;

    @Column(name="shortcode", length = 64)
    private String shortCode;

    @Column(name="ppm_id", length = 64)
    private String ppmId;

    @Column(name="deployment_status", length = 64)
    private String deploymentStatus;

    @Column(length = 255)
    private String title;

    @Column(name="short_desc", length = 512)
    private String shortDesc;

    @Column(name="link_url", length = 1024)
    private String linkUrl;

    // Email-only fields (nullable for SMS)
    @Column(length = 512)
    private String subject;

    @Column(length = 128)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by_id")
    private User modifiedBy;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @Column(name="modified_at")
    private Instant modifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status;

    private Boolean editable = true;

    // reason / comments from QA (optional)
    @Column(name="qa_comments", length = 2000)
    private String qaComments;
}
