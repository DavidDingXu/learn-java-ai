package cn.dingxu.javaai.rag.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class QueryRewriteService {

    public QueryRewriteResult rewrite(String query) {
        String original = query == null ? "" : query.trim();
        Set<String> rewritten = new LinkedHashSet<>();
        List<String> reasons = new ArrayList<>();
        if (!original.isBlank()) {
            rewritten.add(original);
        }

        String normalized = original.replaceAll("\\s+", "");
        if (normalized.contains("退款")) {
            rewritten.add("退款处理规则");
            reasons.add("detected refund scenario");
        }
        if (normalized.contains("发货") || normalized.contains("物流")) {
            rewritten.add("发货后退款处理规则");
            rewritten.add("退款物流状态核对要求");
            reasons.add("detected shipping status");
        }
        if (normalized.contains("高金额") || normalized.contains("大额")) {
            rewritten.add("高金额退款人工复核要求");
            reasons.add("detected high amount risk");
        }

        if (rewritten.isEmpty()) {
            return new QueryRewriteResult(original, List.of(), reasons);
        }
        return new QueryRewriteResult(original, List.copyOf(rewritten), reasons);
    }
}
