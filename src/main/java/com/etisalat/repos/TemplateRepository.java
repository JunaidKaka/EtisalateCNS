// TemplateRepository.java
package com.etisalat.repos;

import com.etisalat.models.Template;
import com.etisalat.models.TemplateStatus;
import com.etisalat.models.TemplateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TemplateRepository extends JpaRepository<Template, Long>, JpaSpecificationExecutor<Template> {

    Page<Template> findByType(TemplateType type, Pageable pageable);

    Page<Template> findByStatus(TemplateStatus status, Pageable pageable);

    Page<Template> findByCreatedBy_Username(String username, Pageable pageable);


    Page<Template> findByTypeAndStatus(TemplateType type, TemplateStatus status, Pageable pageable);

    @Query("""
      SELECT t FROM Template t
      WHERE 
          LOWER(COALESCE(t.code, '')) LIKE :like
         OR LOWER(COALESCE(t.title, '')) LIKE :like
         OR LOWER(COALESCE(t.shortDesc, '')) LIKE :like
         OR LOWER(COALESCE(t.sname, '')) LIKE :like
         OR LOWER(COALESCE(t.senderId, '')) LIKE :like
         OR LOWER(COALESCE(t.shortCode, '')) LIKE :like
    """)
    Page<Template> search(@Param("like") String like, Pageable pageable);

    @Query("SELECT DISTINCT t.sname FROM Template t WHERE t.sname IS NOT NULL ORDER BY t.sname")
    List<String> findDistinctSystemNames();


    @Query("""
    SELECT COALESCE(MAX(t.notid), 0)
    FROM Template t
    WHERE t.sname = :sname
""")
    Integer findMaxNotIdBySystem(@Param("sname") String sname);

}
