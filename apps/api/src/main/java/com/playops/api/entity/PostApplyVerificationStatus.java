package com.playops.api.entity;

/** Phase 5(사후 자동 revert 안전망)에서 사용 예정. 현재는 컬럼만 존재하고 로직은 없음. */
public enum PostApplyVerificationStatus {
    PENDING, PASSED, REGRESSED, SKIPPED
}
