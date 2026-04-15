package com.combotto.controlplane.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.combotto.controlplane.model.AssetEntity;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {

}
