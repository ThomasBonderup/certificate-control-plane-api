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

  @Query("""
      select c
      from CertificateEntity c
      where c.notAfter is not null
        and c.notAfter > :now
        and c.notAfter <= :threshold
        and (:tenantId is null or c.tenantId = :tenantId)
        and (:owner is null or c.owner = :owner)
        and (:renewalStatus is null or c.renewalStatus = :renewalStatus)
      order by c.notAfter asc
      """)
  List<CertificateEntity> findExpiringSoonByFilters(
      @Param("now") OffsetDateTime now,
      @Param("threshold") OffsetDateTime threshold,
      @Param("tenantId") String tenantId,
      @Param("owner") String owner,
      @Param("renewalStatus") RenewalStatus renewalStatus);

  @Query("""
      select c
      from CertificateEntity c
      where
        (c.notAfter is not null and c.notAfter <= :now)
        or
        (
          c.notAfter is not null
          and c.notAfter > :now
          and c.notAfter <= :threshold
          and (
            (c.owner is null or trim(c.owner) = '')
            or c.renewalStatus = :renewalNotStarted
            or c.renewalStatus = :renewalPlanned
            or c.renewalStatus = :renewalInProgress
          )
        )
        or
        c.renewalStatus = :renewalBlocked
      order by c.notAfter asc
      """)
  List<CertificateEntity> findAttentionNeeded(
      @Param("now") OffsetDateTime now,
      @Param("threshold") OffsetDateTime threshold,
      @Param("renewalNotStarted") RenewalStatus renewalNotStarted,
      @Param("renewalPlanned") RenewalStatus renewalPlanned,
      @Param("renewalInProgress") RenewalStatus renewalInProgress,
      @Param("renewalBlocked") RenewalStatus renewalBlocked);

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
