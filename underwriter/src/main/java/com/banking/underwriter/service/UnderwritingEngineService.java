package com.banking.underwriter.service;


import com.banking.underwriter.model.*;
import com.banking.underwriter.repository.UnderwritingPolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UnderwritingEngineService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final UnderwritingPolicyRepository policyRepository;

    public UnderwritingEngineService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
                                     UnderwritingPolicyRepository policyRepository) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.policyRepository = policyRepository;
    }

    public UnderwritingDecision evaluateApplication(String applicationId) {
        log.info("Initiating dynamic underwriting calculation pipeline for Application ID: {}", applicationId);

        // Fetch the active ruleset, or fall back to an explicit baseline policy structure
        UnderwritingPolicy activePolicy = policyRepository.findFirstByIsActiveTrue()
                .orElseGet(() -> {
                    log.warn("No active database policy ruleset discovered. Deploying 'Baseline Default' rules framework.");
                    return new UnderwritingPolicy(1L, "Baseline Default", 30000.0, 45.0, false, true);
                });

        log.info("Applying dynamic verification standard ruleset: {} (Policy ID: {})",
                activePolicy.getPolicyName(), activePolicy.getId());

        // 1. Build the isolated metadata filter expression
        var filterExpression = new FilterExpressionBuilder()
                .eq("applicationId", applicationId)
                .build();

        // 2. Instantiate the advisor securely via its public fluent builder API
        var retrievalAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .filterExpression(filterExpression)
                        .similarityThreshold(0.70)
                        .topK(6).build())
                .build();

        log.debug("Executing Vector RAG context retrieval framework for metadata Application ID: {}", applicationId);

        // 3. Execute prompt evaluation with deterministic JSON Schema extraction mapping
        PolicyCriteriaExtraction criteria = this.chatClient.prompt()
                .advisors(spec -> spec.param("retrievalAdvisor", retrievalAdvisor))
                .user("Analyze the uploaded financial documents for this applicant. Extract " +
                        "total net income, sum up visible monthly obligations, check for document anomalies " +
                        "and flag if any bounced cheques exist.")
                .call()
                .entity(PolicyCriteriaExtraction.class);

        log.info("RAG Model Parsing Success. Results -> Income: {}, Obligations: {}, Bounced Cheques: {}, Anomalies: '{}'",
                criteria.calculatedNetMonthlyIncome(), criteria.totalDetectedMonthlyObligations(),
                criteria.hasRecentBouncedCheques(), criteria.documentAnomalies());

        List<String> causes = new ArrayList<>();
        ApplicationState finalState = ApplicationState.APPROVED;

        // --- Rules Evaluation Core Engine ---

        // Rule A: Minimum Income Verification Check
        if (criteria.calculatedNetMonthlyIncome() < activePolicy.getMinNetIncome()) {
            log.warn("Policy Violation: Net monthly income {} falls below threshold of {}",
                    criteria.calculatedNetMonthlyIncome(), activePolicy.getMinNetIncome());
            causes.add("REJECTION_CAUSE: Income " + criteria.calculatedNetMonthlyIncome() +
                    " is lower than the required threshold of " + activePolicy.getMinNetIncome());
        }

        // Rule B: Debt-to-Income (DTI) Assessment
        double calculatedDti = 0.0;
        if (criteria.calculatedNetMonthlyIncome() > 0) {
            calculatedDti = (criteria.totalDetectedMonthlyObligations() / criteria.calculatedNetMonthlyIncome()) * 100;
        }

        if (calculatedDti > activePolicy.getMaxDtiRatio()) {
            log.warn("Policy Violation: Calculated DTI ratio {}% exceeds ceiling restriction limit of {}%",
                    String.format("%.2f", calculatedDti), activePolicy.getMaxDtiRatio());
            causes.add("REJECTION_CAUSE: Calculated DTI ratio of " + String.format("%.2f", calculatedDti) +
                    "% exceeds dynamic ceiling of " + activePolicy.getMaxDtiRatio() + "%");
        }

        // Rule C: Cheque Processing Deviations
        if (criteria.hasRecentBouncedCheques() && !activePolicy.isAllowBouncedCheques()) {
            log.info("Policy Hold Variant: Cheque bounce recorded on applicant records. Halting automated process routing.");
            causes.add("PENDING_CAUSE: Bounced cheques detected on record. Requires physical review.");
        }

        // Rule D: Integrity and Anomaly Assessment
        if (criteria.documentAnomalies() != null && !criteria.documentAnomalies().isBlank() &&
                !criteria.documentAnomalies().equalsIgnoreCase("none")) {
            log.warn("Data Integrity Exception: Structural inconsistencies surfaced: {}", criteria.documentAnomalies());
            causes.add("PENDING_CAUSE: Document formatting variations reported: " + criteria.documentAnomalies());
        }

        // Consolidated Status Rule Mapping Logic
        if (causes.stream().anyMatch(c -> c.startsWith("REJECTION_CAUSE"))) {
            finalState = ApplicationState.REJECTED;
        } else if (causes.stream().anyMatch(c -> c.startsWith("PENDING_CAUSE"))) {
            finalState = ApplicationState.PENDING;
        }

        if (causes.isEmpty()) {
            causes.add("APPROVAL_CAUSE: Auto-verification evaluation cleared fully against standard rule system: " +
                    activePolicy.getPolicyName());
        }

        log.info("Final automated underwriting pipeline assessment outcome: State={} with {} decision indicators.", finalState, causes.size());
        return new UnderwritingDecision(finalState, causes);
    }
}