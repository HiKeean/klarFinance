package com.api.klarfinance.auth.repository;

 import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.auth.model.RoleMenu;

import java.util.Optional;

 public interface RoleMenuRepository extends JpaRepository<RoleMenu, Integer> {
     Optional<RoleMenu> findByRoleIdAndMenuId(Integer roleId, Integer menuId);
     java.util.List<RoleMenu> findByRoleId(Integer roleId);
     java.util.List<RoleMenu> findByMenuId(Integer menuId);
 }
