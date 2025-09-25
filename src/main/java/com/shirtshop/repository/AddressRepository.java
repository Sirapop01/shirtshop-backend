package com.shirtshop.repository;

import com.shirtshop.entity.Address;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends MongoRepository<Address, String> {
    List<Address> findByUserId(String userId);
    List<Address> findByUserIdAndIsDefaultTrue(String userId);
    Optional<Address> findByIdAndUserId(String id, String userId);
}
