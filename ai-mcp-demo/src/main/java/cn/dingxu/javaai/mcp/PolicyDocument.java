package cn.dingxu.javaai.mcp;

import java.util.Set;

record PolicyDocument(
        String documentId,
        String uri,
        String tenantId,
        Set<String> departments,
        String content
) {
    PolicyDocument {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("uri must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        departments = departments == null ? Set.of() : Set.copyOf(departments);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    boolean accessibleBy(OperatorContext operator) {
        return tenantId.equals(operator.tenantId()) && departments.contains(operator.department());
    }
}
