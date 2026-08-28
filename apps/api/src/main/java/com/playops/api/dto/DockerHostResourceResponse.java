package com.playops.api.dto;

import java.util.List;

public record DockerHostResourceResponse(
        String hostName,
        String osName,
        String osVersion,
        String osArch,
        Integer hostAvailableProcessors,
        Double hostCpuLoadPercent,
        Long hostTotalMemoryBytes,
        Long hostFreeMemoryBytes,
        Long hostUsedMemoryBytes,
        String storagePath,
        Long storageTotalBytes,
        Long storageUsableBytes,
        Long storageUsedBytes,
        Boolean dockerAvailable,
        String dockerServerVersion,
        String dockerOSType,
        String dockerOperatingSystem,
        Integer dockerCpuCount,
        Long dockerMemoryBytes,
        String dockerRootDir,
        List<DockerDiskUsageItem> dockerDiskUsage,
        String message
) {
    public record DockerDiskUsageItem(
            String type,
            String totalCount,
            String activeCount,
            String size,
            String reclaimable
    ) {}
}
