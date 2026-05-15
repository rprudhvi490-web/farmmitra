package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.RoleDto.*;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.Role;
import com.weekendbasket.app.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(r -> new RoleResponse(r.getId(), r.getRoleName(), r.getRoleId()))
                .toList();
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        String roleId = "ROLE_" + request.roleId().toUpperCase().trim();

        if (roleRepository.existsByRoleId(roleId)) {
            throw new WeekendBasketException("Role already exists: " + roleId);
        }

        Role role = Role.builder()
                .roleName(request.roleName())
                .roleId(roleId)
                .build();

        roleRepository.save(role);
        return new RoleResponse(role.getId(), role.getRoleName(), role.getRoleId());
    }
}
