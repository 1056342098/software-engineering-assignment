package com.schoolmanager.backend.user.repo;

import com.schoolmanager.backend.user.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SysRoleRepository extends JpaRepository<SysRole, Long> {
	Optional<SysRole> findByCode(String code);
}
