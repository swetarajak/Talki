// user-profile-service/src/main/java/com/shoppingfeed/profile/controller/UserProfileController.java
package com.shoppingfeed.profile.controller;

import com.shoppingfeed.profile.model.UserProfile;
import com.shoppingfeed.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileRepository profileRepository;

    /**
     * GET /api/v1/profiles/{userId}
     * Returns the current preference profile for a user.
     * The AI engine calls this when building recommendations.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getProfile(@PathVariable String userId) {
        return profileRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/v1/profiles/{userId}
     * GDPR compliance — user requests their data to be deleted
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String userId) {
        profileRepository.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}