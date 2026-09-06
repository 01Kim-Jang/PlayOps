package com.playops.api.dto;

import java.util.List;

/**
 * AI가 생성한 초기 테스트 케이스와, 그 케이스를 곧바로 실행한 execution 정보.
 * estimatedDurationMs는 최근 실행 이력 평균에서 뽑은 예상 소요시간으로, 프론트의 카운트다운 표시에 쓰인다.
 */
public record AiBootstrapResponse(
        Long executionId,
        List<String> generatedFiles,
        String specPath,
        long estimatedDurationMs
) {}
