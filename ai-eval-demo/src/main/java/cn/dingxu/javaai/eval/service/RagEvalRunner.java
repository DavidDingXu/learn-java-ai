package cn.dingxu.javaai.eval.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagEvalRunner {

    public RagEvalReport run(List<RagEvalCase> cases, List<RagEvalObservation> observations) {
        Map<String, RagEvalObservation> observationByCaseId = new LinkedHashMap<>();
        if (observations != null) {
            for (RagEvalObservation observation : observations) {
                observationByCaseId.put(observation.caseId(), observation);
            }
        }

        List<RagEvalResult> results = (cases == null ? List.<RagEvalCase>of() : cases).stream()
                .map(evalCase -> evaluate(evalCase, observationByCaseId.get(evalCase.caseId())))
                .toList();
        return RagEvalReport.from(results);
    }

    private RagEvalResult evaluate(RagEvalCase evalCase, RagEvalObservation observation) {
        if (observation == null) {
            return new RagEvalResult(evalCase.caseId(), false, false, false, false, "missing_observation");
        }

        boolean retrievalPassed = observation.retrievedChunkIds().containsAll(evalCase.expectedChunkIds());
        boolean citationPassed = observation.citedChunkIds().containsAll(evalCase.expectedChunkIds());
        boolean noEvidencePassed = evalCase.expectNoEvidence() && observation.noEvidenceReturned();
        boolean passed = evalCase.expectNoEvidence()
                ? noEvidencePassed
                : retrievalPassed && citationPassed && !observation.noEvidenceReturned();

        String reason = reason(evalCase, observation, retrievalPassed, citationPassed, noEvidencePassed);
        return new RagEvalResult(
                evalCase.caseId(),
                retrievalPassed,
                citationPassed,
                noEvidencePassed,
                passed,
                reason
        );
    }

    private String reason(RagEvalCase evalCase,
                          RagEvalObservation observation,
                          boolean retrievalPassed,
                          boolean citationPassed,
                          boolean noEvidencePassed) {
        if (evalCase.expectNoEvidence()) {
            return noEvidencePassed ? "no_evidence_pass" : "no_evidence_miss";
        }
        if (!retrievalPassed) {
            return "retrieval_miss expected=" + evalCase.expectedChunkIds()
                    + " actual=" + observation.retrievedChunkIds();
        }
        if (!citationPassed) {
            return "citation_miss expected=" + evalCase.expectedChunkIds()
                    + " actual=" + observation.citedChunkIds();
        }
        if (observation.noEvidenceReturned()) {
            return "unexpected_no_evidence";
        }
        return "passed";
    }
}
