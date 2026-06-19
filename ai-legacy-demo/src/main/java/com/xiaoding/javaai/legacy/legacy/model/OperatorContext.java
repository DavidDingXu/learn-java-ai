package com.xiaoding.javaai.legacy.legacy.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class OperatorContext {

    private final String operatorId;
    private final String tenantId;
    private final Set<String> departments;
    private final Set<String> permissions;

    public OperatorContext(String operatorId, String tenantId, Set<String> departments, Set<String> permissions) {
        if (operatorId == null || operatorId.trim().isEmpty()) {
            throw new IllegalArgumentException("operatorId must not be blank");
        }
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        this.operatorId = operatorId;
        this.tenantId = tenantId;
        this.departments = Collections.unmodifiableSet(new HashSet<String>(departments));
        this.permissions = Collections.unmodifiableSet(new HashSet<String>(permissions));
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Set<String> getDepartments() {
        return departments;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean canAccessDepartment(String department) {
        return departments.contains(department);
    }
}
