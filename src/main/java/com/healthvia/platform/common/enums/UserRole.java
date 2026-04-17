// common/enums/UserRole.java
package com.healthvia.platform.common.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    SUPERADMIN("SuperAdmin", "Süper Yönetici"),
    CEO("CEO", "Executive"),
    ADMIN("Admin", "Sistem Yöneticisi"),
    AGENT("Agent", "Satış / Müşteri Temsilcisi"),
    DOCTOR("Doctor", "Doktor"),
    PATIENT("Patient", "Hasta");

    private final String code;
    private final String displayName;

    UserRole(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public boolean isSuperAdmin() {
        return this == SUPERADMIN;
    }

    public boolean isCeo() {
        return this == CEO;
    }

    public boolean isAdmin() {
        return this == ADMIN || this == SUPERADMIN;
    }

    public boolean isAgent() {
        return this == AGENT;
    }

    public boolean isDoctor() {
        return this == DOCTOR;
    }

    public boolean isPatient() {
        return this == PATIENT;
    }

    /** Staff = anyone who works for HealthVia (not patient/doctor). */
    public boolean isStaff() {
        return this == SUPERADMIN || this == CEO || this == ADMIN || this == AGENT;
    }
}