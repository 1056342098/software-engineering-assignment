package com.schoolmanager.backend.user.repo;

import com.schoolmanager.backend.user.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
	Optional<SysUser> findByLoginName(String loginName);

	@Query("select u from SysUser u join u.roles r where r.code = :code order by u.id asc")
	List<SysUser> findByRoleCode(@Param("code") String code);
}
