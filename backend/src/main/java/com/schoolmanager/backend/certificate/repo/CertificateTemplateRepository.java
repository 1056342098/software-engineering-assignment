package com.schoolmanager.backend.certificate.repo;

import com.schoolmanager.backend.certificate.entity.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {
    List<CertificateTemplate> findByEnabledTrueOrderByIdAsc();
}
