package com.baber.identityservice.identityservice.controller;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.Role;
import com.baber.identityservice.identityservice.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;
@RestController
@RequestMapping("/roles")
public class RoleController {
    private final RoleService roleService;
    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }
    @PostMapping("/add")
    public Mono<BaseResponse<Object>> addRole(@RequestBody Role role) {
        return roleService.addRole(role.getName(), role.getDescription())
                .thenReturn(new BaseResponse<>(true, "Role added successfully.", 0, ""
                        , null))
                .onErrorResume(e -> Mono.just(new BaseResponse<>(false, "", 0,
                        "Role already exists.", null)));
    }
    @GetMapping("/all")
    public BaseResponse<List<Role>> getAllRoles() {
        try {
            Flux<Role> rolesFlux = roleService.getAllRoles();
            List<Role> roles = rolesFlux.collectList().block(); // Block and collect into a List
            return new BaseResponse<>(true, "Roles retrieved successfully.", roles.size(), "", roles);
        } catch (Exception e) {
            return new BaseResponse<>(false, "", 0, "Failed to retrieve roles", null);
        }
    }
}
