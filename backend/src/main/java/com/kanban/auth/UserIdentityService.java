package com.kanban.auth;

import com.kanban.auth.dto.UserIdentityResponse;
import com.kanban.exception.ResourceNotFoundException;
import com.kanban.user.User;
import com.kanban.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user social login identities.
 * Handles listing and unlinking OAuth2 provider accounts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserIdentityService {

    private final UserIdentityRepository userIdentityRepository;
    private final UserRepository userRepository;

    /**
     * Get all linked OAuth2 identities for a user.
     *
     * @param userId the user ID
     * @return list of linked identities
     */
    public List<UserIdentityResponse> getUserIdentities(Long userId) {
        log.debug("Getting OAuth2 identities for user: {}", userId);

        List<UserIdentity> identities = userIdentityRepository.findByUserId(userId);

        return identities.stream()
                .map(UserIdentityResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Unlink an OAuth2 identity from a user.
     * Allows unlinking even if it's the last authentication method.
     *
     * @param userId the user ID
     * @param identityId the identity ID to unlink
     * @throws ResourceNotFoundException if identity not found or doesn't belong to user
     */
    @Transactional
    public void unlinkIdentity(Long userId, Long identityId) {
        log.info("Unlinking OAuth2 identity {} for user {}", identityId, userId);

        // Find the identity
        UserIdentity identity = userIdentityRepository.findById(identityId)
                .orElseThrow(() -> new ResourceNotFoundException("UserIdentity", "id", identityId));

        // Verify it belongs to the user
        if (!identity.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("UserIdentity", "id", identityId);
        }

        // Delete the identity (no minimum authentication method check)
        userIdentityRepository.delete(identity);

        log.info("Successfully unlinked OAuth2 identity {} (provider: {}) for user {}",
                identityId, identity.getProvider(), userId);
    }

    /**
     * Check if a user has a password set.
     * Used by frontend to determine if user can unlink all social accounts.
     *
     * @param userId the user ID
     * @return true if user has a password
     */
    public boolean hasPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return user.getPassword() != null && !user.getPassword().isBlank();
    }
}
