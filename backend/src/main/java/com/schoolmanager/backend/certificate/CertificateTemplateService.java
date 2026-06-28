package com.schoolmanager.backend.certificate;

import com.schoolmanager.backend.certificate.entity.CertificateTemplate;
import com.schoolmanager.backend.certificate.repo.CertificateTemplateRepository;
import com.schoolmanager.backend.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CertificateTemplateService {
    private final CertificateTemplateRepository templateRepository;

    public CertificateTemplateService(CertificateTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<CertificateTemplate> listAll() {
        return templateRepository.findAll();
    }

    public List<CertificateTemplate> listEnabled() {
        return templateRepository.findByEnabledTrueOrderByIdAsc();
    }

    @Transactional
    public CertificateTemplate create(String name, String content) {
        CertificateTemplate t = new CertificateTemplate();
        t.setName(name);
        t.setContent(content);
        t.setEnabled(true);
        return templateRepository.save(t);
    }

    @Transactional
    public CertificateTemplate update(long id, String name, String content, Boolean enabled) {
        CertificateTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "未找到该模板"));
        if (name != null) t.setName(name);
        if (content != null) t.setContent(content);
        if (enabled != null) t.setEnabled(enabled);
        return templateRepository.save(t);
    }

    @Transactional
    public void delete(long id) {
        templateRepository.deleteById(id);
    }
}
