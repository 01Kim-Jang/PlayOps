package com.playops.api.service;

import com.playops.api.dto.SlackSettingsRequest;
import com.playops.api.dto.SlackSettingsResponse;
import com.playops.api.entity.SlackSettings;
import com.playops.api.repository.SlackSettingsRepository;
import org.springframework.stereotype.Service;

/**
 * 관리자가 등록하는 플랫폼 공용 Slack Incoming Webhook URL 관리.
 * 평문 URL은 절대 응답으로 재노출하지 않는다 — 마스킹된 상태만 반환한다 (AiProviderSettingsService와 동일 원칙).
 */
@Service
public class SlackSettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final SlackSettingsRepository repository;
    private final SecretCipherService cipherService;

    public SlackSettingsService(SlackSettingsRepository repository, SecretCipherService cipherService) {
        this.repository = repository;
        this.cipherService = cipherService;
    }

    private SlackSettings loadOrCreate() {
        return repository.findById(SETTINGS_ID).orElseGet(SlackSettings::new);
    }

    public SlackSettingsResponse getMaskedSettings() {
        SlackSettings settings = loadOrCreate();
        String plain = decryptOrNull(settings.getWebhookUrlEncrypted());
        return new SlackSettingsResponse(
                plain != null,
                plain != null ? SecretCipherService.mask(plain) : "",
                settings.getUpdatedAt()
        );
    }

    public SlackSettingsResponse updateSettings(SlackSettingsRequest request) {
        SlackSettings settings = loadOrCreate();
        if (request.getWebhookUrl() != null && !request.getWebhookUrl().isBlank()) {
            settings.setWebhookUrlEncrypted(cipherService.encrypt(request.getWebhookUrl().trim()));
        }
        repository.save(settings);
        return getMaskedSettings();
    }

    /** SlackNotificationService 전용 — 평문 webhook URL. 설정 안 됐으면 null (알림은 그냥 조용히 건너뛴다). */
    public String getDecryptedWebhookUrlOrNull() {
        SlackSettings settings = loadOrCreate();
        return decryptOrNull(settings.getWebhookUrlEncrypted());
    }

    private String decryptOrNull(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return null;
        }
        return cipherService.decrypt(encrypted);
    }
}
