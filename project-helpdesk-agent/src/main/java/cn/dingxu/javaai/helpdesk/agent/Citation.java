package cn.dingxu.javaai.helpdesk.agent;

public record Citation(String documentId, String quote) {
    public Citation {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        quote = quote == null ? "" : quote;
    }
}
