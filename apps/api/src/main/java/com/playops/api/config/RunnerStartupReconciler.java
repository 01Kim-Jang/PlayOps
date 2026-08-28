package com.playops.api.config;

import com.playops.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class RunnerStartupReconciler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RunnerStartupReconciler.class);

    private final PlayOpsProperties properties;
    private final ProjectService projectService;

    public RunnerStartupReconciler(PlayOpsProperties properties, ProjectService projectService) {
        this.properties = properties;
        this.projectService = projectService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.dockerEnabled()) {
            return;
        }
        var runners = projectService.reconcileDockerRunners();
        log.info("Reconciled {} Docker runner containers", runners.size());
    }
}
