package com.banking.underwriter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "underwriting_policies")
public class UnderwritingPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String policyName;
    private double minNetIncome;
    private double maxDtiRatio;
    private boolean allowBouncedCheques;
    private boolean isActive;

}