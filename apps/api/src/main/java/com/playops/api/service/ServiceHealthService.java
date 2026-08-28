package com.playops.api.service;

import com.playops.api.dto.ServiceHealthItem;
import com.playops.api.dto.ServiceHealthResponse;
import com.playops.api.entity.DockerStatus;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceHealthService {

    private final DataSource dataSource;
    private final DockerRunnerService dockerRunnerService;

    public ServiceHealthService(DataSource dataSource, DockerRunnerService dockerRunnerService) {
        this.dataSource = dataSource;
        this.dockerRunnerService = dockerRunnerService;
    }

    public ServiceHealthResponse check() {
        List<ServiceHealthItem> services = new ArrayList<>();
        services.add(checkApi());
        services.add(checkDb());
        services.add(checkRunner());

        boolean allOnline = services.stream().allMatch(service -> "ONLINE".equals(service.status()));
        return new ServiceHealthResponse(allOnline ? "ONLINE" : "DEGRADED", Instant.now(), services);
    }

    private ServiceHealthItem checkApi() {
        long started = System.nanoTime();
        return new ServiceHealthItem(
                "api",
                "API",
                "ONLINE",
                "/api",
                elapsedMs(started),
                "Spring API is responding"
        );
    }

    private ServiceHealthItem checkDb() {
        long started = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            return new ServiceHealthItem(
                    "db",
                    "DB",
                    valid ? "ONLINE" : "OFFLINE",
                    connection.getMetaData().getURL(),
                    elapsedMs(started),
                    valid ? "Database connection is valid" : "Database connection is invalid"
            );
        } catch (Exception e) {
            return new ServiceHealthItem(
                    "db",
                    "DB",
                    "OFFLINE",
                    "datasource",
                    elapsedMs(started),
                    e.getMessage()
            );
        }
    }

    private ServiceHealthItem checkRunner() {
        long started = System.nanoTime();
        try {
            if (!dockerRunnerService.isDockerAvailable()) {
                return new ServiceHealthItem(
                        "runner",
                        "Runner",
                        "OFFLINE",
                        "docker",
                        elapsedMs(started),
                        "Docker is disabled or unavailable"
                );
            }

            var runners = dockerRunnerService.listRunnerContainers();
            long running = runners.stream()
                    .filter(runner -> runner.dockerStatus() == DockerStatus.RUNNING)
                    .count();
            return new ServiceHealthItem(
                    "runner",
                    "Runner",
                    "ONLINE",
                    "docker",
                    elapsedMs(started),
                    running + "/" + runners.size() + " runner containers running"
            );
        } catch (Exception e) {
            return new ServiceHealthItem(
                    "runner",
                    "Runner",
                    "OFFLINE",
                    "docker",
                    elapsedMs(started),
                    "Docker check: " + e.getMessage()
            );
        }
    }

    private long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }
}
