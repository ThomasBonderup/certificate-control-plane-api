package com.combotto.controlplane.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.combotto.controlplane.model.CertificateEntity;

interface CertificateRepository extends JpaRepository<CertificateEntity, UUID> {
  
}
