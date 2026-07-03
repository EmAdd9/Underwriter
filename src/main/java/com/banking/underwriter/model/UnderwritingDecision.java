package com.banking.underwriter.model;

import java.util.List;

public record UnderwritingDecision(
        ApplicationState state,
        List<String> causes
) {}