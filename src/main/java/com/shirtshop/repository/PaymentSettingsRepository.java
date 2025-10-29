// src/main/java/com/shirtshop/repository/PaymentSettingsRepository.java
package com.shirtshop.repository;

import com.shirtshop.entity.PaymentSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentSettingsRepository extends MongoRepository<PaymentSettings, String> {}
