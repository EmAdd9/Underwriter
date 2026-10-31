package com.banking.underwriter.service;


import com.banking.underwriter.model.*;
import com.banking.underwriter.repository.UnderwritingPolicyRepository;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class UnderwritingEngineService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final UnderwritingPolicyRepository policyRepository;

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
                        .similarityThreshold(0.5)
                        .topK(6).build())
                .build();

        log.debug("Executing Vector RAG context retrieval framework for metadata Application ID: {}", applicationId);

        // 3. Execute prompt evaluation with deterministic JSON Schema extraction mapping
        PolicyCriteriaExtraction criteria = this.chatClient.prompt()
                .advisors(retrievalAdvisor)
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
        minIncomeVerificationCheck(criteria, activePolicy, causes);

        // Rule B: Debt-to-Income (DTI) Assessment
        assesDebtToIncome(criteria, activePolicy, causes);

        // Rule C: Cheque Processing Deviations
        fetchChequeDeviations(criteria, activePolicy, causes);

        // Rule D: Integrity and Anomaly Assessment
        assesIntegrityAndAnomaly(criteria, causes);

        // Consolidated Status Rule Mapping Logic
        finalState = consolidateStatusRuleMappingAndFinalState(causes, finalState, activePolicy);

        log.info("Final automated underwriting pipeline assessment outcome: State={} with {} decision indicators.", finalState, causes.size());
        return new UnderwritingDecision(finalState, causes);
    }

    private static ApplicationState consolidateStatusRuleMappingAndFinalState(List<String> causes, ApplicationState finalState, UnderwritingPolicy activePolicy) {
        if (causes.stream().anyMatch(c -> c.startsWith("REJECTION_CAUSE"))) {
            finalState = ApplicationState.REJECTED;
        } else if (causes.stream().anyMatch(c -> c.startsWith("PENDING_CAUSE"))) {
            finalState = ApplicationState.PENDING;
        }

        if (causes.isEmpty()) {
            causes.add("APPROVAL_CAUSE: Auto-verification evaluation cleared fully against standard rule system: " +
                    activePolicy.getPolicyName());
        }
        return finalState;
    }

    private static void assesIntegrityAndAnomaly(PolicyCriteriaExtraction criteria, List<String> causes) {
        if (criteria.documentAnomalies() != null && !criteria.documentAnomalies().isBlank() &&
                !criteria.documentAnomalies().equalsIgnoreCase("none")) {
            log.warn("Data Integrity Exception: Structural inconsistencies surfaced: {}", criteria.documentAnomalies());
            causes.add("PENDING_CAUSE: Document formatting variations reported: " + criteria.documentAnomalies());
        }
    }

    private static void fetchChequeDeviations(PolicyCriteriaExtraction criteria, UnderwritingPolicy activePolicy, List<String> causes) {
        if (criteria.hasRecentBouncedCheques() && !activePolicy.isAllowBouncedCheques()) {
            log.info("Policy Hold Variant: Cheque bounce recorded on applicant records. Halting automated process routing.");
            causes.add("PENDING_CAUSE: Bounced cheques detected on record. Requires physical review.");
        }
    }

    private static void assesDebtToIncome(PolicyCriteriaExtraction criteria, UnderwritingPolicy activePolicy, List<String> causes) {
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
    }

    private static void minIncomeVerificationCheck(PolicyCriteriaExtraction criteria, UnderwritingPolicy activePolicy, List<String> causes) {
        if (criteria.calculatedNetMonthlyIncome() < activePolicy.getMinNetIncome()) {
            log.warn("Policy Violation: Net monthly income {} falls below threshold of {}",
                    criteria.calculatedNetMonthlyIncome(), activePolicy.getMinNetIncome());
            causes.add("REJECTION_CAUSE: Income " + criteria.calculatedNetMonthlyIncome() +
                    " is lower than the required threshold of " + activePolicy.getMinNetIncome());
        }
    }
}