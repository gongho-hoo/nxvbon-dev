package com.nxvibeon.backend.codechange.repository;

import com.nxvibeon.backend.codechange.domain.CodeChangeProposalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeChangeProposalJpaRepository extends JpaRepository<CodeChangeProposalEntity, String> {
}
