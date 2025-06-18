package com.baber.identityservice.identityservice.controller;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.Permission;
import com.baber.identityservice.identityservice.service.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PermissionController.class)
public class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PermissionService permissionService;

    @Autowired
    private ObjectMapper objectMapper;

    private Permission permission;
    private List<Permission> permissionList;

    @BeforeEach
    public void setUp() {
        permission = new Permission();
        permission.setId(1L);
        permission.setName("READ_PRIVILEGES");

        permissionList = Collections.singletonList(permission);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetDefaultPermissions() throws Exception {
        when(permissionService.getDefaultPermissions(eq(1L))).thenReturn(permissionList);

        mockMvc.perform(get("/permissions/defaults/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Default permissions retrieved"))
                .andExpect(jsonPath("$.data[0].name").value("READ_PRIVILEGES"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetPermissionsBySalonAndRole() throws Exception {
        Set<Permission> permissionSet = Set.of(permission);
        when(permissionService.getPermissionsBySalonAndRole(eq(1L), eq("Admin"))).thenReturn(permissionSet);

        mockMvc.perform(get("/permissions/salon/1/role/Admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Permissions retrieved"))
                .andExpect(jsonPath("$.data[0].name").value("READ_PRIVILEGES"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testUpdatePermissionsBySalonAndRole() throws Exception {
        when(permissionService.updatePermissionsBySalonAndRole(eq(1L), eq("Admin"), any()))
                .thenReturn(permissionList);

        mockMvc.perform(put("/permissions/salon/1/role/Admin")
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permissionList)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Permissions updated"))
                .andExpect(jsonPath("$.data[0].name").value("READ_PRIVILEGES"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreatePermissions() throws Exception {
        when(permissionService.findByName(any())).thenReturn(Optional.empty());
        when(permissionService.save(any())).thenReturn(permission);

        mockMvc.perform(post("/permissions/add")
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permissionList)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Permissions processed"))
                .andExpect(jsonPath("$.data[0].name").value("READ_PRIVILEGES"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAddPermissionToRole() throws Exception {
        when(permissionService.addPermissionToRole(eq(1L), eq("Admin"), any())).thenReturn(permissionList);

        mockMvc.perform(post("/permissions/salon/1/role/Admin/add")
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Permission added successfully"))
                .andExpect(jsonPath("$.data[0].name").value("READ_PRIVILEGES"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testRemovePermissionFromRole() throws Exception {
        when(permissionService.removePermissionFromRole(eq(1L), eq(1L), eq(1L))).thenReturn(permissionList);

        mockMvc.perform(delete("/permissions/salon/1/role/1/permission/1/remove")
                        .with(csrf())) // Add CSRF token
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Permission removed successfully"))
                .andExpect(jsonPath("$.data[0].name").value("READ_PRIVILEGES"));
    }
}
