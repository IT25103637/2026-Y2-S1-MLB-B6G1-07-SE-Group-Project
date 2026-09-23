package com.safari.wildlife.repository;

import com.safari.wildlife.entity.Guide;
import com.safari.wildlife.enums.GuideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuideRepository extends JpaRepository<Guide, Long> {
    Optional<Guide> findByLicenseNumberIgnoreCase(String licenseNumber);
    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);
    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, Long id);
    List<Guide> findByStatus(GuideStatus status);
}
