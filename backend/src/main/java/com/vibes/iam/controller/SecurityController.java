package com.vibes.iam.controller;

import com.vibes.iam.dto.PasswordChangeRequest;
import com.vibes.iam.service.PasswordPolicyService;
import com.vibes.iam.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/security")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Security", description = "Security management APIs")
public class SecurityController {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change current user's password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                           Authentication authentication) {
        boolean success = securityService.changePassword(
                authentication.getName(), 
                request.getOldPassword(), 
                request.getNewPassword()
        );

        if (success) {
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to change password"));
        }
    }

    @PostMapping("/validate-password")
    @Operation(summary = "Validate password", description = "Validate password against security policies")
    public ResponseEntity<?> validatePassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        PasswordPolicyService.PasswordValidationResult result = passwordPolicyService.validatePassword(password);
        PasswordPolicyService.PasswordStrength strength = passwordPolicyService.calculatePasswordStrength(password);

        return ResponseEntity.ok(Map.of(
                "valid", result.isValid(),
                "errors", result.getErrors(),
                "strength", strength
        ));
    }

    @PostMapping("/force-password-reset/{username}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @Operation(summary = "Force password reset", description = "Force password reset for a user")
    public ResponseEntity<?> forcePasswordReset(@PathVariable String username,
                                               Authentication authentication) {
        securityService.forcePasswordReset(username, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Password reset forced for user: " + username));
    }

    @GetMapping("/account-status/{username}")
    @PreAuthorize("hasAuthority('READ_USERS') or authentication.name == #username")
    @Operation(summary = "Check account status", description = "Check if account is locked")
    public ResponseEntity<?> checkAccountStatus(@PathVariable String username) {
        boolean isLocked = securityService.isAccountLocked(username);
        return ResponseEntity.ok(Map.of("locked", isLocked));
    }

    @GetMapping("/password-policy")
    @Operation(summary = "Get password policy", description = "Get current password policy requirements")
    public ResponseEntity<?> getPasswordPolicy() {
        return ResponseEntity.ok(Map.of(
                "minLength", 8,
                "requireUppercase", true,
                "requireLowercase", true,
                "requireNumbers", true,
                "requireSpecialChars", true
        ));
    }
}