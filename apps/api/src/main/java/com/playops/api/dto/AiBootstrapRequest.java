package com.playops.api.dto;

/** 프로젝트 등록 직후 AI에게 초기 테스트 케이스 생성을 맡길 때의 요청. */
public record AiBootstrapRequest(String instruction, String provider) {}
