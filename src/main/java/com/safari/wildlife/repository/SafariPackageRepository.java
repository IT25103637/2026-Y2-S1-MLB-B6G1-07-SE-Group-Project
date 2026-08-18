package com.safari.wildlife.repository;

import com.safari.wildlife.entity.SafariPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafariPackageRepository extends JpaRepository<SafariPackage, Long> {
    List<SafariPackage> findByDestinationIdAndIsActiveTrue(Long destinationId);
    List<SafariPackage> findByIsActiveTrue();
}
