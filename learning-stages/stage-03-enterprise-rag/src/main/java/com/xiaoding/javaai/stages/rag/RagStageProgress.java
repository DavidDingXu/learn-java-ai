package com.xiaoding.javaai.stages.rag;

record RagStageProgress(
        String allowedDocumentId,
        int allowedVersion,
        long allowedRevision,
        String blockedDocumentId,
        int blockedVersion,
        long blockedRevision,
        String allowedIndexTaskId,
        String blockedIndexTaskId,
        boolean indexed
) {

    static RagStageProgress empty() {
        return new RagStageProgress(null, 0, 0, null, 0, 0, null, null, false);
    }

    boolean uploaded() {
        return present(allowedDocumentId) && present(blockedDocumentId);
    }

    boolean published() {
        return uploaded() && present(allowedIndexTaskId) && present(blockedIndexTaskId);
    }

    RagStageProgress withAllowedUpload(String documentId, int version, long revision) {
        return new RagStageProgress(
                documentId, version, revision,
                blockedDocumentId, blockedVersion, blockedRevision,
                allowedIndexTaskId, blockedIndexTaskId, indexed
        );
    }

    RagStageProgress withBlockedUpload(String documentId, int version, long revision) {
        return new RagStageProgress(
                allowedDocumentId, allowedVersion, allowedRevision,
                documentId, version, revision,
                allowedIndexTaskId, blockedIndexTaskId, indexed
        );
    }

    RagStageProgress withAllowedIndexTask(String indexTaskId) {
        return new RagStageProgress(
                allowedDocumentId, allowedVersion, allowedRevision,
                blockedDocumentId, blockedVersion, blockedRevision,
                indexTaskId, blockedIndexTaskId, indexed
        );
    }

    RagStageProgress withBlockedIndexTask(String indexTaskId) {
        return new RagStageProgress(
                allowedDocumentId, allowedVersion, allowedRevision,
                blockedDocumentId, blockedVersion, blockedRevision,
                allowedIndexTaskId, indexTaskId, indexed
        );
    }

    RagStageProgress markIndexed() {
        if (!published()) {
            throw new IllegalStateException("第 15 篇需要先完成第 13、14 篇");
        }
        return new RagStageProgress(
                allowedDocumentId, allowedVersion, allowedRevision,
                blockedDocumentId, blockedVersion, blockedRevision,
                allowedIndexTaskId, blockedIndexTaskId, true
        );
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
