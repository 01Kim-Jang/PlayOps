package com.playops.api.repository;

import com.playops.api.entity.SlackSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlackSettingsRepository extends JpaRepository<SlackSettings, Long> {
}
