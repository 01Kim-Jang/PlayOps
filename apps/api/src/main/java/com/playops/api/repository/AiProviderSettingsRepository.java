package com.playops.api.repository;

import com.playops.api.entity.AiProviderSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderSettingsRepository extends JpaRepository<AiProviderSettings, Long> {
}
