package com.safari.wildlife.controller;

import com.safari.wildlife.entity.Destination;
import com.safari.wildlife.entity.PackageOption;
import com.safari.wildlife.entity.SafariPackage;
import com.safari.wildlife.entity.SafariSchedule;
import com.safari.wildlife.repository.PackageOptionRepository;
import com.safari.wildlife.repository.SafariPackageRepository;
import com.safari.wildlife.service.DestinationService;
import com.safari.wildlife.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicApiController {

    private final DestinationService destinationService;
    private final SafariPackageRepository packageRepository;
    private final PackageOptionRepository optionRepository;
    private final ScheduleService scheduleService;

    @Autowired
    public PublicApiController(DestinationService destinationService,
                               SafariPackageRepository packageRepository,
                               PackageOptionRepository optionRepository,
                               ScheduleService scheduleService) {
        this.destinationService = destinationService;
        this.packageRepository = packageRepository;
        this.optionRepository = optionRepository;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/destinations")
    public ResponseEntity<List<Destination>> getDestinations() {
        return ResponseEntity.ok(destinationService.getActiveDestinations());
    }

    @GetMapping("/destinations/{id}")
    public ResponseEntity<Destination> getDestination(@PathVariable Long id) {
        return ResponseEntity.ok(destinationService.getDestinationById(id));
    }

    @GetMapping("/packages")
    public ResponseEntity<List<SafariPackage>> getPackages(@RequestParam(required = false) Long destinationId) {
        if (destinationId != null) {
            return ResponseEntity.ok(packageRepository.findByDestinationIdAndIsActiveTrue(destinationId));
        }
        return ResponseEntity.ok(packageRepository.findByIsActiveTrue());
    }

    @GetMapping("/packages/{id}")
    public ResponseEntity<Map<String, Object>> getPackageDetails(@PathVariable Long id) {
        SafariPackage pkg = packageRepository.findById(id).orElse(null);
        if (pkg == null) {
            return ResponseEntity.notFound().build();
        }
        List<PackageOption> options = optionRepository.findBySafariPackageIdAndIsActiveTrue(id);

        Map<String, Object> result = new HashMap<>();
        result.put("package", pkg);
        result.put("options", options);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<SafariSchedule>> getSchedules(@RequestParam(required = false) Long destinationId) {
        if (destinationId != null) {
            return ResponseEntity.ok(scheduleService.getSchedulesByDestination(destinationId));
        }
        return ResponseEntity.ok(scheduleService.getUpcomingSchedules());
    }
}
