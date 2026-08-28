package com.playops.api.repository;

import com.playops.api.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, String> {
    boolean existsByProjectId(String projectId);
}
