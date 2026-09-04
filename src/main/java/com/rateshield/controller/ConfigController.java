package com.rateshield.controller;

import com.rateshield.model.RateLimitConfig;
import com.rateshield.repository.RateLimitConfigRepository;
import com.rateshield.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/management/config")
@RequiredArgsConstructor
public class ConfigController {

    private final RateLimitConfigRepository rateLimitConfigRepository;
    private final RateLimitService rateLimitService;

    @GetMapping("/")
    public List<RateLimitConfig> getAllConfigs() {
        return rateLimitConfigRepository.findAll();
    }

    @GetMapping("/{id}")
    public RateLimitConfig getConfigById(@PathVariable Long id) {
        return rateLimitConfigRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Config not found"));
    }

    @PostMapping("/")
    public RateLimitConfig createConfig(@RequestBody RateLimitConfig config) {
        RateLimitConfig saved = rateLimitConfigRepository.save(config);
        rateLimitService.clearCache();
        return saved;
    }

    @PutMapping("/{id}")
    public RateLimitConfig updateConfig(@PathVariable Long id, @RequestBody RateLimitConfig config) {
        if (!rateLimitConfigRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Config not found");
        }
        config.setId(id);
        RateLimitConfig updated = rateLimitConfigRepository.save(config);
        rateLimitService.clearCache();
        return updated;
    }

    @DeleteMapping("/{id}")
    public void deleteConfig(@PathVariable Long id) {
        if (!rateLimitConfigRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Config not found");
        }
        rateLimitConfigRepository.deleteById(id);
        rateLimitService.clearCache();
    }
}
