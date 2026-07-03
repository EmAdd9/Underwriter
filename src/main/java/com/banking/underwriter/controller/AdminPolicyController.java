package com.banking.underwriter.controller;


import com.banking.underwriter.model.UnderwritingPolicy;
import com.banking.underwriter.repository.UnderwritingPolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/policies")
public class AdminPolicyController {

    private final UnderwritingPolicyRepository policyRepository;

    public AdminPolicyController(UnderwritingPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @PostMapping
    public ResponseEntity<UnderwritingPolicy> createPolicy(@RequestBody UnderwritingPolicy newPolicy) {
        log.info("HTTP POST -> Admin invocation received to generate new criteria policy rule schema configurations.");
        if (newPolicy.isActive()) {
            policyRepository.findFirstByIsActiveTrue().ifPresent(p -> {
                log.info("Deactivating currently active operational policy: ID={}", p.getId());
                p.setActive(false);
                policyRepository.save(p);
            });
        }
        UnderwritingPolicy savedPolicy = policyRepository.save(newPolicy);
        log.info("Successfully persisted new policy parameters under System ID reference: {}", savedPolicy.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPolicy);
    }

    @GetMapping
    public ResponseEntity<List<UnderwritingPolicy>> viewAllPolicies() {
        log.info("HTTP GET -> Retrieving list of all structural validation underwriting rule-sets configured inside system ledger.");
        return ResponseEntity.ok(policyRepository.findAll());
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> toggleActiveRuleSet(@PathVariable Long id) {
        log.info("HTTP PUT -> Request initiated to switch active rule parameters template context to target criteria ID: {}", id);

        policyRepository.findFirstByIsActiveTrue().ifPresent(p -> {
            log.debug("Disabling rules configuration reference ID: {}", p.getId());
            p.setActive(false);
            policyRepository.save(p);
        });

        UnderwritingPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Failed to alter activation states. Policy Configuration context ID {} not located.", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Target policy parameters not found");
                });

        policy.setActive(true);
        policyRepository.save(policy);
        log.info("System rule activation target switched successfully. Operational Policy ID is now: {}", id);
        return ResponseEntity.noContent().build();
    }
}