package com.banking.underwriter.model;

public record PolicyCriteriaExtraction(
        boolean employerVerified,
        double calculatedNetMonthlyIncome,
        double totalDetectedMonthlyObligations,
        boolean hasRecentBouncedCheques,
        String documentAnomalies
) {}