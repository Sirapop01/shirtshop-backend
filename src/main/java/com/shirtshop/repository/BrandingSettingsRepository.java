// src/main/java/com/shirtshop/repository/BrandingSettingsRepository.java
package com.shirtshop.repository;

import com.shirtshop.entity.BrandingSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BrandingSettingsRepository extends MongoRepository<BrandingSettings, String> {
}
