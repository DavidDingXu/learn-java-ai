package cn.dingxu.javaai.enterprise.rag;

public record OperatorScope(String tenantId, String department) {
    public OperatorScope {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (department == null || department.isBlank()) {
            throw new IllegalArgumentException("department must not be blank");
        }
    }
}
