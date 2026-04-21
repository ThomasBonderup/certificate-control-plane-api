package com.combotto.controlplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.combotto.controlplane.api.CertificateRenewalStatusChangedEvent;
import com.combotto.controlplane.api.CertificateResponse;
import com.combotto.controlplane.api.UpdateCertificateRequest;
import com.combotto.controlplane.common.CertificateMapper;
import com.combotto.controlplane.common.CurrentTenantProvider;
import com.combotto.controlplane.common.CurrentUserProvider;
import com.combotto.controlplane.common.TenantAccessValidator;
import com.combotto.controlplane.model.CertificateEntity;
import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;
import com.combotto.controlplane.repositories.CertificateRepository;
import com.combotto.controlplane.services.CertificateEventPublisher;
import com.combotto.controlplane.services.CertificateService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

  private static final String TENANT_ID = "demo-tenant";
  private static final String CURRENT_USER = "test-user";

  @Mock
  private CertificateRepository certificateRepository;

  @Mock
  private CertificateMapper certificateMapper;

  @Mock
  private CurrentUserProvider currentUserProvider;

  @Mock
  private CurrentTenantProvider currentTenantProvider;

  @Mock
  private TenantAccessValidator tenantAccessValidator;

  @Mock
  private CertificateEventPublisher certificateEventPublisher;

  private CertificateService certificateService;

  @BeforeEach
  void setUp() {
    certificateService = new CertificateService(
        certificateRepository,
        certificateMapper,
        currentUserProvider,
        currentTenantProvider,
        tenantAccessValidator,
        new SimpleMeterRegistry(),
        certificateEventPublisher);

    when(currentTenantProvider.getRequiredTenantId()).thenReturn(TENANT_ID);
    when(currentUserProvider.getCurrentUserId()).thenReturn(CURRENT_USER);
    when(certificateMapper.toResponse(any(CertificateEntity.class))).thenReturn(mock(CertificateResponse.class));
    when(certificateRepository.save(any(CertificateEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void updatePublishesEventWhenRenewalStatusChanges() {
    UUID certificateId = UUID.randomUUID();
    CertificateEntity entity = existingCertificate(certificateId, RenewalStatus.NOT_STATUS);
    UpdateCertificateRequest request = new UpdateCertificateRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        RenewalStatus.IN_PROGRESS,
        null,
        null,
        null);

    when(certificateRepository.findByIdAndTenantId(certificateId, TENANT_ID))
        .thenReturn(Optional.of(entity));

    certificateService.update(certificateId, request);

    verify(certificateRepository).save(entity);
    verify(certificateEventPublisher, times(1))
        .publishRenewalStatusChanged(any(CertificateRenewalStatusChangedEvent.class));
  }

  @Test
  void updateDoesNotPublishEventWhenRenewalStatusIsUnchanged() {
    UUID certificateId = UUID.randomUUID();
    CertificateEntity entity = existingCertificate(certificateId, RenewalStatus.IN_PROGRESS);
    UpdateCertificateRequest request = new UpdateCertificateRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        RenewalStatus.IN_PROGRESS,
        null,
        "ops-team",
        null);

    when(certificateRepository.findByIdAndTenantId(certificateId, TENANT_ID))
        .thenReturn(Optional.of(entity));

    certificateService.update(certificateId, request);

    verify(certificateRepository).save(entity);
    assertEquals("ops-team", entity.getOwner());
    verify(certificateEventPublisher, never())
        .publishRenewalStatusChanged(any(CertificateRenewalStatusChangedEvent.class));
  }

  @Test
  void updateDoesNotPublishEventWhenRenewalStatusIsOmitted() {
    UUID certificateId = UUID.randomUUID();
    CertificateEntity entity = existingCertificate(certificateId, RenewalStatus.IN_PROGRESS);
    UpdateCertificateRequest request = new UpdateCertificateRequest(
        "Updated certificate name",
        null,
        null,
        null,
        null,
        null,
        null,
        CertificateStatus.ACTIVE,
        null,
        null,
        null,
        "Updated notes");

    when(certificateRepository.findByIdAndTenantId(certificateId, TENANT_ID))
        .thenReturn(Optional.of(entity));

    certificateService.update(certificateId, request);

    verify(certificateRepository).save(entity);
    assertEquals("Updated certificate name", entity.getName());
    assertEquals("Updated notes", entity.getNotes());
    assertEquals(RenewalStatus.IN_PROGRESS, entity.getRenewalStatus());
    verify(certificateEventPublisher, never())
        .publishRenewalStatusChanged(any(CertificateRenewalStatusChangedEvent.class));
  }

  @Test
  void updatePublishesCorrectRenewalStatusChangedEvent() {
    UUID certificateId = UUID.randomUUID();
    CertificateEntity entity = existingCertificate(certificateId, RenewalStatus.PLANNED);
    UpdateCertificateRequest request = new UpdateCertificateRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        RenewalStatus.BLOCKED,
        "Waiting on vendor",
        null,
        null);

    when(certificateRepository.findByIdAndTenantId(certificateId, TENANT_ID))
        .thenReturn(Optional.of(entity));

    ArgumentCaptor<CertificateRenewalStatusChangedEvent> captor =
        ArgumentCaptor.forClass(CertificateRenewalStatusChangedEvent.class);

    certificateService.update(certificateId, request);

    verify(certificateEventPublisher).publishRenewalStatusChanged(captor.capture());

    CertificateRenewalStatusChangedEvent event = captor.getValue();
    assertEquals(certificateId, event.certificateId());
    assertEquals(TENANT_ID, event.tenantId());
    assertEquals("PLANNED", event.oldRenewalStatus());
    assertEquals("BLOCKED", event.newRenewalStatus());
    assertEquals("Waiting on vendor", event.blockedReason());
    assertEquals(CURRENT_USER, event.updatedBy());
    assertNotNull(event.renewalUpdatedAt());
    assertNotNull(event.occurredAt());
  }

  private CertificateEntity existingCertificate(UUID id, RenewalStatus renewalStatus) {
    CertificateEntity entity = new CertificateEntity();
    entity.setId(id);
    entity.setTenantId(TENANT_ID);
    entity.setName("Existing certificate");
    entity.setStatus(CertificateStatus.ACTIVE);
    entity.setRenewalStatus(renewalStatus);
    entity.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
    entity.setUpdatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
    entity.setCreatedBy("seed-user");
    entity.setUpdatedBy("seed-user");
    return entity;
  }
}
