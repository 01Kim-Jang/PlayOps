package com.playops.api.repository;

import com.playops.api.entity.ScenarioCaseSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioCaseSettingRepository extends JpaRepository<ScenarioCaseSetting, Long> {
    List<ScenarioCaseSetting> findByProjectId(String projectId);

    Optional<ScenarioCaseSetting> findByProjectIdAndNodeTypeAndSpecPathAndGrepFilter(
            String projectId,
            String nodeType,
            String specPath,
            String grepFilter
    );
}
