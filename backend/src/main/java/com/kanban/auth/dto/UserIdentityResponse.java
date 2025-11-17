package com.kanban.auth.dto;

import com.kanban.auth.OAuth2Provider;
import com.kanban.auth.UserIdentity;

import java.time.LocalDateTime;

/**
 * Response DTO for OAuth2 user identity information.
 */
public record UserIdentityResponse(
        Long id,
        OAuth2Provider provider,
        String providerUserId,
        String email,
        String name,
        String profileImageUrl,
        LocalDateTime createdAt
) {
    /**
     * Convert UserIdentity entity to response DTO.
     *
     * @param identity the UserIdentity entity
     * @return UserIdentityResponse DTO
     */
    public static UserIdentityResponse from(UserIdentity identity) {
        return new UserIdentityResponse(
                identity.getId(),
                identity.getProvider(),
                identity.getProviderUserId(),
                identity.getEmail(),
                identity.getName(),
                identity.getProfileImageUrl(),
                identity.getCreatedAt()
        );
    }
}
