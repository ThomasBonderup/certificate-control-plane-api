package com.combotto.controlplane.support;

import com.combotto.controlplane.model.AssetEntity;
import com.combotto.controlplane.repositories.AssetRepository;

public final class AssetFixtures {

  private static long nextId = 1L;

  private AssetFixtures() {
  }

  public static Long createAndReturnId(AssetRepository assetRepository) {
    return createAndReturnId(assetRepository, 1001L, "Primary Gateway Asset");
  }

  public static Long createAndReturnId(
      AssetRepository assetRepository,
      Long companyId,
      String name) {
    AssetEntity asset = activeAsset(companyId, name);
    return assetRepository.save(asset).getId();
  }

  public static Long createDeletedAndReturnId(AssetRepository assetRepository, String name) {
    AssetEntity asset = activeAsset(1001L, name);
    asset.setDeleted(true);
    return assetRepository.save(asset).getId();
  }

  public static AssetEntity activeAsset(Long companyId, String name) {
    AssetEntity asset = new AssetEntity();
    asset.setId(nextId++);
    asset.setCompanyId(companyId);
    asset.setAssetType("gateway");
    asset.setName(name);
    asset.setExternalRef("mqtt://" + slugify(name) + ".example.com:1883");
    asset.setProtocol("mqtt");
    asset.setSiteLabel("lab");
    asset.setDeleted(false);
    return asset;
  }

  private static String slugify(String value) {
    return value.toLowerCase().replace(" ", "-");
  }
}
