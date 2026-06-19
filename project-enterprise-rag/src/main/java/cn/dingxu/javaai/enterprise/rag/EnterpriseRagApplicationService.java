package cn.dingxu.javaai.enterprise.rag;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class EnterpriseRagApplicationService {

    private static final String NO_EVIDENCE = "没有检索到当前用户可访问的依据。";
    private static final String CONFLICTED_EVIDENCE = "检索到相互冲突的制度依据，需要人工确认后再生成处理建议。";

    private final InMemoryDocumentRepository documentRepository;
    private final InMemoryChunkRepository chunkRepository;
    private final PolicyDocumentChunker chunker;
    private final HybridPolicyRetriever retriever;
    private final EvidenceGovernanceService evidenceGovernanceService;

    public EnterpriseRagApplicationService(InMemoryDocumentRepository documentRepository,
                                           InMemoryChunkRepository chunkRepository,
                                           PolicyDocumentChunker chunker) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunker = chunker;
        this.retriever = new HybridPolicyRetriever(chunkRepository);
        this.evidenceGovernanceService = new EvidenceGovernanceService();
    }

    public static EnterpriseRagApplicationService seeded() {
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        InMemoryChunkRepository chunkRepository = new InMemoryChunkRepository();
        return new EnterpriseRagApplicationService(documentRepository, chunkRepository, new PolicyDocumentChunker());
    }

    public IndexTask uploadAndIndex(PolicyDocumentUpload upload) {
        PolicyDocument document = documentRepository.saveNextVersion(upload);
        List<DocumentChunk> chunks = chunker.chunk(document);
        chunkRepository.replaceDocumentChunks(document.documentId(), document.version(), chunks);
        return new IndexTask(
                "idx-" + UUID.randomUUID(),
                document.documentId(),
                document.version(),
                IndexTaskStatus.COMPLETED,
                chunks.size(),
                Instant.now()
        );
    }

    public RagAnswer answer(String question, OperatorScope scope) {
        String rewrittenQuery = rewriteQuery(question);
        List<DocumentChunk> chunks = retriever.retrieve(rewrittenQuery, scope);
        EvidenceGovernanceDecision governance = evidenceGovernanceService.select(chunks, Instant.now(), 3);

        RagTrace trace = new RagTrace("trace-" + UUID.randomUUID(), List.of(
                new RagTraceStep("query-rewrite", rewrittenQuery),
                new RagTraceStep("access-filter", scope.tenantId() + "/" + scope.department()),
                new RagTraceStep("hybrid-retrieve", "matched=" + chunks.size()),
                new RagTraceStep("evidence-governance", governanceDetail(governance)),
                new RagTraceStep("citation-compose", "citations=" + governance.selectedEvidence().size())
        ));

        if (governance.status() == EvidenceGovernanceStatus.CONFLICTED) {
            return new RagAnswer(CONFLICTED_EVIDENCE, List.of(), trace);
        }

        if (governance.selectedEvidence().isEmpty()) {
            return new RagAnswer(NO_EVIDENCE, List.of(), trace);
        }

        String content = governance.selectedEvidence().stream()
                .map(DocumentChunk::content)
                .reduce((left, right) -> left + "\n" + right)
                .orElse(NO_EVIDENCE);
        List<Citation> citations = governance.selectedEvidence().stream()
                .map(chunk -> new Citation(chunk.documentId(), chunk.chunkId(), chunk.content()))
                .toList();
        return new RagAnswer(content, citations, trace);
    }

    public EvalReport evaluate(List<EvalCase> cases) {
        if (cases == null || cases.isEmpty()) {
            return new EvalReport(0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        int retrievalHits = 0;
        int citationHits = 0;
        for (EvalCase evalCase : cases) {
            List<DocumentChunk> retrieved = retriever.retrieve(rewriteQuery(evalCase.question()), evalCase.scope());
            if (retrieved.stream().anyMatch(chunk -> chunk.documentId().equals(evalCase.expectedDocumentId()))) {
                retrievalHits++;
            }
            RagAnswer answer = answer(evalCase.question(), evalCase.scope());
            if (answer.citations().stream().anyMatch(citation -> citation.documentId().equals(evalCase.expectedDocumentId()))) {
                citationHits++;
            }
        }

        return new EvalReport(
                cases.size(),
                rate(retrievalHits, cases.size()),
                rate(citationHits, cases.size())
        );
    }

    public InMemoryDocumentRepository documentRepository() {
        return documentRepository;
    }

    public InMemoryChunkRepository chunkRepository() {
        return chunkRepository;
    }

    private BigDecimal rate(int hits, int total) {
        return BigDecimal.valueOf(hits).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private String rewriteQuery(String question) {
        if (question == null) {
            return "";
        }
        return question
                .replace("客户申请", "")
                .replace("订单已发货", "发货")
                .replace("但", "")
                .trim();
    }

    private String governanceDetail(EvidenceGovernanceDecision governance) {
        return "status=" + governance.status()
                + ", selected=" + governance.selectedEvidence().stream().map(DocumentChunk::chunkId).toList()
                + ", rejected=" + governance.rejectedChunkIds()
                + ", reasons=" + governance.reasons();
    }
}
