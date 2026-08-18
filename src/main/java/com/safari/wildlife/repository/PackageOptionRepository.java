package com.safari.wildlife.repository;

import com.safari.wildlife.entity.PackageOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageOptionRepository extends JpaRepository<PackageOption, Long> {
    List<PackageOption> findBySafariPackageIdAndIsActiveTrue(Long packageId);
    List<PackageOption> findBySafariPackageId(Long packageId);
}
