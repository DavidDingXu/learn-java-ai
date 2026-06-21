package com.xiaoding.javaai.rag.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RagLabService {

    private static final String DEFAULT_QUERY = "客户申请退款但订单已发货怎么办";
    private static final String DEFAULT_MARKDOWN = """
            # 退款制度
            ## 发货后退款
            客户申请退款但订单已经发货时，客服必须先核对物流状态，确认包裹是否签收。
            ## 高金额退款
            单笔退款金额超过 3000 元时，需要人工复核后再继续处理。
            """;

    private final DocumentParser parser;
    private final DocumentChunker chunker;
    private final DocumentAccessFilter accessFilter;
    private final EmbeddingProvider embeddingProvider;
    private final RerankService rerankService;
    private final QueryRewriteService queryRewriteService;
    private final ContextCompressor contextCompressor;
    private final IndexTaskService indexTaskService;

    public RagLabService(DocumentParser parser,
                         DocumentChunker chunker,
                         DocumentAccessFilter accessFilter,
                         EmbeddingProvider embeddingProvider,
                         RerankService rerankService,
                         QueryRewriteService queryRewriteService,
                         ContextCompressor contextCompressor,
                         IndexTaskService indexTaskService) {
        this.parser = parser;
        this.chunker = chunker;
        this.accessFilter = accessFilter;
        this.embeddingProvider = embeddingProvider;
        this.rerankService = rerankService;
        this.queryRewriteService = queryRewriteService;
        this.contextCompressor = contextCompressor;
        this.indexTaskService = indexTaskService;
    }

    public PipelineLabResult pipeline(PipelineLabRequest request) {
        DocumentMetadata metadata = metadata(request);
        ParsedDocument document = parser.parseMarkdown(markdown(request.markdown()), metadata);
        List<DocumentChunk> chunks = chunker(request.maxChunkChars()).chunk(document);
        return new PipelineLabResult(
                metadataView(metadata),
                document.plainText(),
                chunks.size(),
                chunks.stream().map(this::chunkView).toList()
        );
    }

    public AccessLabResult access(AccessLabRequest request) {
        OperatorScope scope = scope(request.tenantId(), request.department());
        List<DocumentChunk> chunks = sampleChunks();
        return new AccessLabResult(
                scopeView(scope),
                chunks.stream().map(chunk -> accessFilter.decide(chunk, scope)).toList(),
                chunks.stream().map(this::chunkView).toList()
        );
    }

    public RetrievalLabResult retrieval(RetrievalLabRequest request) {
        String query = query(request.query());
        OperatorScope scope = scope(request.tenantId(), request.department());
        int topK = topK(request.topK());
        HybridRetriever retriever = localHybridRetriever(sampleChunks());
        List<HybridRetrievalResult> hybridResults = retriever.search(query, queryVector(query), scope, topK);
        List<RerankedRetrievalResult> rerankedResults = rerankService.rerank(query, hybridResults, scope, topK);
        return new RetrievalLabResult(
                query,
                scopeView(scope),
                hybridResults.stream().map(this::retrievalView).toList(),
                rerankedResults.stream().map(this::rerankView).toList()
        );
    }

    public RewriteLabResult rewrite(RewriteLabRequest request) {
        String query = query(request.query());
        OperatorScope scope = scope(request.tenantId(), request.department());
        int topK = topK(request.topK());
        HybridRetriever retriever = localHybridRetriever(sampleChunks());
        MultiQueryRagRetriever multiQueryRetriever = new MultiQueryRagRetriever(queryRewriteService, retriever);
        MultiQueryRagRetrievalResult result = multiQueryRetriever.retrieve(query, queryVector(query), scope, topK);
        ContextCompressionResult compressed = contextCompressor.compress(
                result.results(),
                request.maxCharsPerChunk() == null ? 56 : Math.max(8, request.maxCharsPerChunk())
        );
        return new RewriteLabResult(
                result.rewrite().originalQuery(),
                result.rewrite().queries(),
                result.rewrite().reasons(),
                result.results().stream().map(this::multiQueryView).toList(),
                compressed.items(),
                compressed.droppedChunkIds()
        );
    }

    public IndexTaskResult index(IndexLabRequest request) {
        String documentId = request.documentId() == null || request.documentId().isBlank()
                ? "refund-policy-lab"
                : request.documentId();
        return indexTaskService.rebuild(documentId, markdown(request.markdown()));
    }

    private DocumentMetadata metadata(PipelineLabRequest request) {
        return new DocumentMetadata(
                valueOrDefault(request.documentId(), "refund-policy-lab"),
                valueOrDefault(request.tenantId(), "tenant-a"),
                valueOrDefault(request.title(), "退款制度样例"),
                valueOrDefault(request.docType(), "POLICY"),
                departments(request.departments()),
                valueOrDefault(request.version(), "v1")
        );
    }

    private DocumentChunker chunker(Integer maxChunkChars) {
        if (maxChunkChars == null) {
            return chunker;
        }
        return new PolicyDocumentChunker(maxChunkChars);
    }

    private HybridRetriever localHybridRetriever(List<DocumentChunk> chunks) {
        InMemoryVectorIndex vectorIndex = new InMemoryVectorIndex(accessFilter);
        EmbeddingBatchService embeddingBatchService = new EmbeddingBatchService(embeddingProvider, 4);
        List<EmbeddingResult> embeddings = embeddingBatchService.embed(chunks);
        vectorIndex.upsert(embeddings.stream()
                .map(embedding -> new VectorIndexEntry(findChunk(chunks, embedding.chunkId()), embedding.vector()))
                .toList());
        return new HybridRetriever(chunks, vectorIndex, accessFilter);
    }

    private DocumentChunk findChunk(List<DocumentChunk> chunks, String chunkId) {
        return chunks.stream()
                .filter(chunk -> chunk.chunkId().equals(chunkId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("chunk not found: " + chunkId));
    }

    private List<Double> queryVector(String query) {
        List<List<Double>> vectors = embeddingProvider.embed(List.of(query));
        return vectors.isEmpty() ? List.of() : vectors.get(0);
    }

    private List<DocumentChunk> sampleChunks() {
        return List.of(
                new DocumentChunk(
                        "refund-policy-001",
                        "refund-policy-001-c1",
                        "tenant-a",
                        Set.of("support"),
                        List.of("退款制度", "发货后退款"),
                        "POLICY",
                        "v3",
                        "客户申请退款但订单已经发货时，客服必须先核对物流状态，确认包裹是否签收。高金额退款需要人工复核。"
                ),
                new DocumentChunk(
                        "refund-policy-001",
                        "refund-policy-001-c2",
                        "tenant-a",
                        Set.of("support"),
                        List.of("退款制度", "高金额退款"),
                        "POLICY",
                        "v3",
                        "单笔退款金额超过 3000 元时，客服不能直接关闭工单，需要提交人工复核。"
                ),
                new DocumentChunk(
                        "hr-policy-001",
                        "hr-policy-001-c1",
                        "tenant-a",
                        Set.of("hr"),
                        List.of("假勤制度"),
                        "POLICY",
                        "v1",
                        "年假审批由直属主管和 HR 共同确认，客服部门不可查看明细。"
                ),
                new DocumentChunk(
                        "refund-policy-002",
                        "refund-policy-002-c1",
                        "tenant-b",
                        Set.of("support"),
                        List.of("退款制度"),
                        "POLICY",
                        "v1",
                        "租户 B 的退款制度只能被租户 B 的客服查询。"
                )
        );
    }

    private OperatorScope scope(String tenantId, String department) {
        return new OperatorScope(valueOrDefault(tenantId, "tenant-a"), valueOrDefault(department, "support"));
    }

    private OperatorScopeView scopeView(OperatorScope scope) {
        return new OperatorScopeView(scope.tenantId(), scope.department());
    }

    private DocumentMetadataView metadataView(DocumentMetadata metadata) {
        return new DocumentMetadataView(
                metadata.documentId(),
                metadata.tenantId(),
                metadata.title(),
                metadata.docType(),
                metadata.departments().stream().toList(),
                metadata.version()
        );
    }

    private ChunkView chunkView(DocumentChunk chunk) {
        return new ChunkView(
                chunk.documentId(),
                chunk.chunkId(),
                chunk.tenantId(),
                chunk.departments().stream().toList(),
                chunk.headingPath(),
                chunk.docType(),
                chunk.version(),
                chunk.content()
        );
    }

    private RetrievalView retrievalView(HybridRetrievalResult result) {
        DocumentChunk chunk = result.chunk();
        return new RetrievalView(
                chunk.documentId(),
                chunk.chunkId(),
                chunk.headingPath(),
                chunk.content(),
                result.score(),
                result.sources().stream().toList()
        );
    }

    private RerankView rerankView(RerankedRetrievalResult result) {
        DocumentChunk chunk = result.chunk();
        return new RerankView(
                chunk.documentId(),
                chunk.chunkId(),
                chunk.headingPath(),
                chunk.content(),
                result.originalScore(),
                result.rerankScore(),
                result.sources().stream().toList(),
                result.reasons()
        );
    }

    private MultiQueryView multiQueryView(MultiQueryRetrievalItem item) {
        DocumentChunk chunk = item.chunk();
        return new MultiQueryView(
                chunk.documentId(),
                chunk.chunkId(),
                chunk.content(),
                item.score(),
                item.sources().stream().toList(),
                item.matchedQueries()
        );
    }

    private Set<String> departments(List<String> departments) {
        if (departments == null || departments.isEmpty()) {
            return Set.of("support");
        }
        return new LinkedHashSet<>(departments);
    }

    private String query(String query) {
        return valueOrDefault(query, DEFAULT_QUERY);
    }

    private String markdown(String markdown) {
        return valueOrDefault(markdown, DEFAULT_MARKDOWN);
    }

    private int topK(Integer topK) {
        if (topK == null) {
            return 3;
        }
        return Math.max(1, Math.min(topK, 5));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record PipelineLabRequest(
            String documentId,
            String tenantId,
            String title,
            String docType,
            List<String> departments,
            String version,
            String markdown,
            Integer maxChunkChars
    ) {
    }

    public record PipelineLabResult(
            DocumentMetadataView metadata,
            String plainText,
            int chunkCount,
            List<ChunkView> chunks
    ) {
    }

    public record AccessLabRequest(String tenantId, String department) {
    }

    public record AccessLabResult(
            OperatorScopeView scope,
            List<DocumentAccessDecision> decisions,
            List<ChunkView> chunks
    ) {
    }

    public record RetrievalLabRequest(String query, String tenantId, String department, Integer topK) {
    }

    public record RetrievalLabResult(
            String query,
            OperatorScopeView scope,
            List<RetrievalView> hybridResults,
            List<RerankView> rerankedResults
    ) {
    }

    public record RewriteLabRequest(
            String query,
            String tenantId,
            String department,
            Integer topK,
            Integer maxCharsPerChunk
    ) {
    }

    public record RewriteLabResult(
            String originalQuery,
            List<String> rewrittenQueries,
            List<String> rewriteReasons,
            List<MultiQueryView> retrievalItems,
            List<CompressedContextItem> compressedItems,
            List<String> droppedChunkIds
    ) {
    }

    public record IndexLabRequest(String documentId, String markdown) {
    }

    public record OperatorScopeView(String tenantId, String department) {
    }

    public record DocumentMetadataView(
            String documentId,
            String tenantId,
            String title,
            String docType,
            List<String> departments,
            String version
    ) {
    }

    public record ChunkView(
            String documentId,
            String chunkId,
            String tenantId,
            List<String> departments,
            List<String> headingPath,
            String docType,
            String version,
            String content
    ) {
    }

    public record RetrievalView(
            String documentId,
            String chunkId,
            List<String> headingPath,
            String content,
            double score,
            List<String> sources
    ) {
    }

    public record RerankView(
            String documentId,
            String chunkId,
            List<String> headingPath,
            String content,
            double originalScore,
            double rerankScore,
            List<String> sources,
            List<String> reasons
    ) {
    }

    public record MultiQueryView(
            String documentId,
            String chunkId,
            String content,
            double score,
            List<String> sources,
            List<String> matchedQueries
    ) {
    }
}
