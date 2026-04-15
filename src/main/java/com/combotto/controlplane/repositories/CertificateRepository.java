package com.combotto.controlplane.repositories;

import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.combotto.controlplane.model.CertificateEntity;
import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;

public interface CertificateRepository extends JpaRepository<CertificateEntity, UUID> {

  @Query("""
          select c
          from CertificateEntity c
          where (:tenantId is null or c.tenantId = :tenantId)
            and (:status is null or c.status = :status)
            and (:renewalStatus is null or c.renewalStatus = :renewalStatus)
          order by c.createdAt desc
      """)
  List<CertificateEntity> findByFilters(
      @Param("tenantId") String tenantId,
      @Param("status") CertificateStatus status,
      @Param("renewalStatus") RenewalStatus renewalStatus);

  long countByStatus(CertificateStatus status);

  @Query("""
      select count(c)
      from CertificateEntity c
      where c.notAfter is not null
        and c.notAfter > :now
        and c.notAfter <= :threshold
      """)
  long countExpiringSoon(
      @Param("now") OffsetDateTime now,
      @Param("threshold") OffsetDateTime threshold);

  long countByRenewalStatus(RenewalStatus renewalStatus);
}