package com.combotto.controlplane.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.combotto.controlplane.model.OutboxEventEntity;
import com.combotto.controlplane.model.OutboxStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
  List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
