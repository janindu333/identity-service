package com.baber.identityservice.config;

import com.baber.identityservice.entity.Permission;
import com.baber.identityservice.repository.PermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Order(2) // Run after KeycloakRoleSync (Order 1), before StartupConfig (Order 3+)
public class RolesAndPermissionsInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(RolesAndPermissionsInitializer.class);
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing permissions (roles are synced from Keycloak)...");
        
        try {
            // Create all permissions first
            createAllPermissions();
            
            logger.info("Permission initialization completed successfully!");
            
        } catch (Exception e) {
            logger.error("Failed to initialize permissions", e);
            // Don't throw - allow service to start even if initialization fails
            logger.warn("Service will continue without permission initialization.");
        }
    }
    
    private Map<String, Permission> createAllPermissions() {
        Map<String, Permission> permissionMap = new HashMap<>();
        
        // Super Admin Permissions
        permissionMap.put("view_all_salons", createPermissionIfNotExists("view_all_salons", "View all salons across the platform"));
        permissionMap.put("create_edit_delete_any_salon", createPermissionIfNotExists("create_edit_delete_any_salon", "Create/edit/delete any salon"));
        permissionMap.put("manage_all_users", createPermissionIfNotExists("manage_all_users", "Manage all users (owners, managers, staff, clients)"));
        permissionMap.put("access_all_appointments", createPermissionIfNotExists("access_all_appointments", "Access all appointments and bookings"));
        permissionMap.put("view_platform_analytics", createPermissionIfNotExists("view_platform_analytics", "View platform-wide analytics and reports"));
        permissionMap.put("manage_subscription_plans", createPermissionIfNotExists("manage_subscription_plans", "Manage subscription plans and billing"));
        permissionMap.put("handle_support_tickets", createPermissionIfNotExists("handle_support_tickets", "Handle support tickets and disputes"));
        permissionMap.put("suspend_activate_salon_accounts", createPermissionIfNotExists("suspend_activate_salon_accounts", "Suspend/activate salon accounts"));
        permissionMap.put("configure_platform_settings", createPermissionIfNotExists("configure_platform_settings", "Configure platform settings"));
        permissionMap.put("access_system_logs", createPermissionIfNotExists("access_system_logs", "Access system logs and audit trails"));
        permissionMap.put("moderate_reviews", createPermissionIfNotExists("moderate_reviews", "Moderate reviews and reports"));
        permissionMap.put("send_platform_notifications", createPermissionIfNotExists("send_platform_notifications", "Send platform-wide notifications"));
        
        // Owner Permissions
        permissionMap.put("create_salon_locations", createPermissionIfNotExists("create_salon_locations", "Create new salon locations"));
        permissionMap.put("edit_delete_own_salons", createPermissionIfNotExists("edit_delete_own_salons", "Edit/delete own salons"));
        permissionMap.put("view_salon_settings", createPermissionIfNotExists("view_salon_settings", "View all salon settings"));
        permissionMap.put("manage_business_hours", createPermissionIfNotExists("manage_business_hours", "Manage salon business hours"));
        permissionMap.put("create_edit_delete_services", createPermissionIfNotExists("create_edit_delete_services", "Create/edit/delete services and pricing"));
        permissionMap.put("invite_managers", createPermissionIfNotExists("invite_managers", "Invite salon managers"));
        permissionMap.put("invite_staff", createPermissionIfNotExists("invite_staff", "Invite staff members"));
        permissionMap.put("invite_receptionists", createPermissionIfNotExists("invite_receptionists", "Invite receptionists"));
        permissionMap.put("remove_managers_staff_receptionists", createPermissionIfNotExists("remove_managers_staff_receptionists", "Remove managers/staff/receptionists"));
        permissionMap.put("view_all_appointments", createPermissionIfNotExists("view_all_appointments", "View all appointments (all locations)"));
        permissionMap.put("create_edit_cancel_appointments", createPermissionIfNotExists("create_edit_cancel_appointments", "Create/edit/cancel appointments"));
        permissionMap.put("view_all_client_data", createPermissionIfNotExists("view_all_client_data", "View all client data"));
        permissionMap.put("export_client_database", createPermissionIfNotExists("export_client_database", "Export client database"));
        permissionMap.put("view_financial_reports", createPermissionIfNotExists("view_financial_reports", "View financial reports and revenue"));
        permissionMap.put("access_detailed_analytics", createPermissionIfNotExists("access_detailed_analytics", "Access detailed analytics"));
        permissionMap.put("configure_payment_settings", createPermissionIfNotExists("configure_payment_settings", "Configure payment settings"));
        permissionMap.put("set_booking_policies", createPermissionIfNotExists("set_booking_policies", "Set booking policies (cancellation, deposits)"));
        permissionMap.put("manage_salon_branding", createPermissionIfNotExists("manage_salon_branding", "Manage salon branding (logo, photos)"));
        permissionMap.put("configure_notification_preferences", createPermissionIfNotExists("configure_notification_preferences", "Configure notification preferences"));
        permissionMap.put("view_staff_performance_metrics", createPermissionIfNotExists("view_staff_performance_metrics", "View staff performance metrics"));
        permissionMap.put("manage_subscription_billing", createPermissionIfNotExists("manage_subscription_billing", "Manage subscription and billing"));
        permissionMap.put("set_staff_commissions", createPermissionIfNotExists("set_staff_commissions", "Set staff commissions and payroll"));
        permissionMap.put("view_respond_reviews", createPermissionIfNotExists("view_respond_reviews", "View and respond to reviews"));
        
        // Manager Permissions
        permissionMap.put("view_assigned_salon_locations", createPermissionIfNotExists("view_assigned_salon_locations", "View assigned salon location(s) only"));
        permissionMap.put("edit_salon_details", createPermissionIfNotExists("edit_salon_details", "Edit salon details (assigned locations)"));
        permissionMap.put("manage_business_hours_assigned", createPermissionIfNotExists("manage_business_hours_assigned", "Manage business hours (assigned locations)"));
        permissionMap.put("edit_services_pricing", createPermissionIfNotExists("edit_services_pricing", "Edit services and pricing (cannot delete)"));
        permissionMap.put("remove_staff_receptionists", createPermissionIfNotExists("remove_staff_receptionists", "Remove staff/receptionists (with restrictions)"));
        permissionMap.put("view_appointments_assigned", createPermissionIfNotExists("view_appointments_assigned", "View all appointments (assigned locations)"));
        permissionMap.put("manage_staff_schedules", createPermissionIfNotExists("manage_staff_schedules", "Manage staff schedules"));
        permissionMap.put("view_client_database_assigned", createPermissionIfNotExists("view_client_database_assigned", "View client database (assigned locations)"));
        permissionMap.put("add_client_notes", createPermissionIfNotExists("add_client_notes", "Add client notes"));
        permissionMap.put("view_limited_financial_reports", createPermissionIfNotExists("view_limited_financial_reports", "View limited financial reports (no detailed revenue)"));
        permissionMap.put("process_walk_in_bookings", createPermissionIfNotExists("process_walk_in_bookings", "Process walk-in bookings"));
        permissionMap.put("handle_customer_complaints", createPermissionIfNotExists("handle_customer_complaints", "Handle customer complaints"));
        permissionMap.put("view_staff_availability", createPermissionIfNotExists("view_staff_availability", "View staff availability"));
        permissionMap.put("approve_reject_time_off", createPermissionIfNotExists("approve_reject_time_off", "Approve/reject time-off requests"));
        
        // Staff Permissions
        permissionMap.put("view_own_schedule", createPermissionIfNotExists("view_own_schedule", "View own schedule and appointments"));
        permissionMap.put("accept_decline_appointment_requests", createPermissionIfNotExists("accept_decline_appointment_requests", "Accept/decline appointment requests"));
        permissionMap.put("mark_appointments_completed", createPermissionIfNotExists("mark_appointments_completed", "Mark appointments as completed"));
        permissionMap.put("mark_appointments_no_show", createPermissionIfNotExists("mark_appointments_no_show", "Mark appointments as no-show"));
        permissionMap.put("block_personal_time_off", createPermissionIfNotExists("block_personal_time_off", "Block personal time off (vacation, breaks)"));
        permissionMap.put("set_personal_availability", createPermissionIfNotExists("set_personal_availability", "Set personal availability hours"));
        permissionMap.put("view_assigned_client_details", createPermissionIfNotExists("view_assigned_client_details", "View assigned client details"));
        permissionMap.put("add_service_notes", createPermissionIfNotExists("add_service_notes", "Add service notes to appointments"));
        permissionMap.put("view_client_service_history", createPermissionIfNotExists("view_client_service_history", "View client service history"));
        permissionMap.put("view_own_earnings", createPermissionIfNotExists("view_own_earnings", "View own earnings/commissions"));
        permissionMap.put("update_own_profile", createPermissionIfNotExists("update_own_profile", "Update own profile (bio, photo, specialties)"));
        permissionMap.put("view_own_performance_metrics", createPermissionIfNotExists("view_own_performance_metrics", "View own performance metrics"));
        permissionMap.put("receive_booking_notifications", createPermissionIfNotExists("receive_booking_notifications", "Receive booking notifications"));
        permissionMap.put("view_other_staff_schedules_readonly", createPermissionIfNotExists("view_other_staff_schedules_readonly", "View other staff schedules (read-only)"));
        
        // Receptionist Permissions
        permissionMap.put("view_all_staff_schedules_readonly", createPermissionIfNotExists("view_all_staff_schedules_readonly", "View all staff schedules (read-only)"));
        permissionMap.put("book_appointments", createPermissionIfNotExists("book_appointments", "Book appointments for clients"));
        permissionMap.put("create_walk_in_client_accounts", createPermissionIfNotExists("create_walk_in_client_accounts", "Create walk-in client accounts"));
        permissionMap.put("cancel_reschedule_appointments", createPermissionIfNotExists("cancel_reschedule_appointments", "Cancel/reschedule appointments"));
        permissionMap.put("process_payments", createPermissionIfNotExists("process_payments", "Process payments (cash, card)"));
        permissionMap.put("check_in_clients", createPermissionIfNotExists("check_in_clients", "Check-in clients for appointments"));
        permissionMap.put("view_daily_appointment_list", createPermissionIfNotExists("view_daily_appointment_list", "View daily appointment list"));
        permissionMap.put("view_client_database", createPermissionIfNotExists("view_client_database", "View client database"));
        permissionMap.put("add_edit_client_contact_info", createPermissionIfNotExists("add_edit_client_contact_info", "Add/edit client contact information"));
        permissionMap.put("send_appointment_reminders", createPermissionIfNotExists("send_appointment_reminders", "Send appointment reminders"));
        permissionMap.put("handle_phone_inquiries", createPermissionIfNotExists("handle_phone_inquiries", "Handle phone inquiries"));
        permissionMap.put("print_receipts_invoices", createPermissionIfNotExists("print_receipts_invoices", "Print receipts and invoices"));
        permissionMap.put("view_service_menu_pricing", createPermissionIfNotExists("view_service_menu_pricing", "View service menu and pricing"));
        permissionMap.put("apply_discounts", createPermissionIfNotExists("apply_discounts", "Apply discounts (if authorized)"));
        
        // Client Permissions
        permissionMap.put("browse_search_salons", createPermissionIfNotExists("browse_search_salons", "Browse and search salons"));
        permissionMap.put("view_salon_details_services", createPermissionIfNotExists("view_salon_details_services", "View salon details and services"));
        permissionMap.put("view_staff_profiles_availability", createPermissionIfNotExists("view_staff_profiles_availability", "View staff profiles and availability"));
        permissionMap.put("book_appointments_online", createPermissionIfNotExists("book_appointments_online", "Book appointments online"));
        permissionMap.put("view_own_upcoming_appointments", createPermissionIfNotExists("view_own_upcoming_appointments", "View own upcoming appointments"));
        permissionMap.put("view_own_past_appointments", createPermissionIfNotExists("view_own_past_appointments", "View own past appointments"));
        permissionMap.put("cancel_appointments_within_policy", createPermissionIfNotExists("cancel_appointments_within_policy", "Cancel appointments (within policy limits)"));
        permissionMap.put("reschedule_appointments_within_policy", createPermissionIfNotExists("reschedule_appointments_within_policy", "Reschedule appointments (within policy limits)"));
        permissionMap.put("add_appointments_to_calendar", createPermissionIfNotExists("add_appointments_to_calendar", "Add appointments to calendar"));
        permissionMap.put("receive_booking_confirmations", createPermissionIfNotExists("receive_booking_confirmations", "Receive booking confirmations"));
        permissionMap.put("receive_appointment_reminders", createPermissionIfNotExists("receive_appointment_reminders", "Receive appointment reminders"));
        permissionMap.put("view_own_service_history", createPermissionIfNotExists("view_own_service_history", "View own service history"));
        permissionMap.put("save_favorite_salons", createPermissionIfNotExists("save_favorite_salons", "Save favorite salons"));
        permissionMap.put("save_favorite_staff", createPermissionIfNotExists("save_favorite_staff", "Save favorite staff members"));
        permissionMap.put("leave_reviews_ratings", createPermissionIfNotExists("leave_reviews_ratings", "Leave reviews and ratings"));
        permissionMap.put("upload_profile_photo", createPermissionIfNotExists("upload_profile_photo", "Upload profile photo"));
        permissionMap.put("manage_payment_methods", createPermissionIfNotExists("manage_payment_methods", "Manage payment methods"));
        permissionMap.put("view_receipts_invoices", createPermissionIfNotExists("view_receipts_invoices", "View receipts and invoices"));
        permissionMap.put("update_own_contact_info", createPermissionIfNotExists("update_own_contact_info", "Update own contact information"));
        permissionMap.put("set_notification_preferences", createPermissionIfNotExists("set_notification_preferences", "Set notification preferences"));
        permissionMap.put("request_appointment_changes", createPermissionIfNotExists("request_appointment_changes", "Request appointment changes"));
        
        logger.info("Created/verified {} permissions", permissionMap.size());
        return permissionMap;
    }
    
    private Permission createPermissionIfNotExists(String name, String description) {
        Optional<Permission> existing = permissionRepository.findByName(name);
        if (existing.isPresent()) {
            logger.debug("Permission '{}' already exists", name);
            return existing.get();
        }
        
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDeleted(0);
        Permission saved = permissionRepository.save(permission);
        logger.info("Created permission: {}", name);
        return saved;
    }
}
