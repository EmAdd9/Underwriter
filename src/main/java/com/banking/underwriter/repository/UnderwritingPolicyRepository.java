package com.banking.underwriter.repository;

import com.banking.underwriter.model.UnderwritingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnderwritingPolicyRepository extends JpaRepository <UnderwritingPolicy,Long>{
    Optional<UnderwritingPolicy> findFirstByIsActiveTrue();
}
