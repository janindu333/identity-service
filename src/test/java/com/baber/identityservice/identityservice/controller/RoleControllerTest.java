package com.baber.identityservice.identityservice.controller;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.Permission;
import com.baber.identityservice.identityservice.entity.Role;
import com.baber.identityservice.identityservice.service.PermissionService;
import com.baber.identityservice.identityservice.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(RoleController.class)
public class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @MockBean
    private PermissionService permissionService;

    @Autowired
    private ObjectMapper objectMapper;

    private Role role;
    private List<Role> roleList;

    @BeforeEach
    public void setUp() {
        role = new Role();
        role.setId(1);
        role.setName("Admin");

        roleList = Arrays.asList(role);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetAllRoles() throws Exception {
        when(roleService.getAllRoles()).thenReturn(roleList);

        mockMvc.perform(get("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Roles retrieved successfully"))
                .andExpect(jsonPath("$.data[0].name").value("Admin"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetRoleById() throws Exception {
        when(roleService.getRoleById(1)).thenReturn(Optional.of(role));

        mockMvc.perform(get("/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Role retrieved successfully"))
                .andExpect(jsonPath("$.data.name").value("Admin"));
    }

//    @Test
//    @WithMockUser(username = "admin", roles = {"ADMIN"})
//    public void testGetRoleByIdNotFound() throws Exception {
//        when(roleService.getRoleById(1)).thenReturn(Optional.empty());
//
//        mockMvc.perform(get("/roles/1"))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Role not found"))
//                .andExpect(jsonPath("$.error").value("Role with id 1 not found"));
//    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreateRole() throws Exception {
        when(roleService.createRole(any())).thenReturn(Optional.of(role));

        mockMvc.perform(post("/roles/add")
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Role created successfully"))
                .andExpect(jsonPath("$.data.name").value("Admin"));
    }

//    @Test
//    @WithMockUser(username = "admin", roles = {"ADMIN"})
//    public void testCreateRoleAlreadyExists() throws Exception {
//        when(roleService.createRole(any())).thenReturn(Optional.empty());
//
//        mockMvc.perform(post("/roles/add")
//                        .with(csrf()) // Add CSRF token
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(role)))
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Role already exists"))
//                .andExpect(jsonPath("$.error").value("Role with name Admin already exists"));
//    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testUpdateRole() throws Exception {
        when(roleService.updateRole(eq(1), any())).thenReturn(Optional.of(role));

        mockMvc.perform(put("/roles/1")
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Role updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Admin"));
    }

//    @Test
//    @WithMockUser(username = "admin", roles = {"ADMIN"})
//    public void testUpdateRoleNotFound() throws Exception {
//        when(roleService.updateRole(eq(1), any())).thenReturn(Optional.empty());
//
//        mockMvc.perform(put("/roles/1")
//                        .with(csrf()) // Add CSRF token
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(role)))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Role not found"))
//                .andExpect(jsonPath("$.error").value("Role with id 1 not found"));
//    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDeleteRole() throws Exception {
        when(roleService.deleteRole(1)).thenReturn(true);

        mockMvc.perform(delete("/roles/1/remove")
                        .with(csrf())) // Add CSRF token
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Role deleted successfully"));
    }

//    @Test
//    @WithMockUser(username = "admin", roles = {"ADMIN"})
//    public void testDeleteRoleNotFound() throws Exception {
//        when(roleService.deleteRole(1)).thenReturn(false);
//
//        mockMvc.perform(delete("/roles/1/remove")
//                        .with(csrf())) // Add CSRF token
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Role not found"))
//                .andExpect(jsonPath("$.error").value("Role with id 1 not found"));
//    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetRoleByNameAndSalonId() throws Exception {
        when(roleService.findByNameAndSalonId("Admin", 1L)).thenReturn(Optional.of(role));

        mockMvc.perform(get("/roles/salon/1/role/Admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetRoleByNameAndSalonIdNotFound() throws Exception {
        when(roleService.findByNameAndSalonId("Admin", 1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/roles/salon/1/role/Admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testUpdateRolePermissions() throws Exception {
        Permission permission = new Permission();
        permission.setId(1L);
        permission.setName("READ_PRIVILEGES");

        List<Permission> permissions = Arrays.asList(permission);

        when(roleService.getRoleById(1)).thenReturn(Optional.of(role));
        when(roleService.createRole(any())).thenReturn(Optional.of(role));

        mockMvc.perform(put("/roles/1/permissions")
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permissions)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Role update successfully"));
    }

}
