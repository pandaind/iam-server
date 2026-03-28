package com.vibes.iam.service;

import com.vibes.iam.service.PasswordPolicyService.PasswordValidationResult;
import com.vibes.iam.service.PasswordPolicyService.PasswordStrength;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordPolicyServiceTest {

    private PasswordPolicyService passwordPolicyService;

    @BeforeEach
    void setUp() {
        passwordPolicyService = new PasswordPolicyService();
        ReflectionTestUtils.setField(passwordPolicyService, "minLength", 8);
        ReflectionTestUtils.setField(passwordPolicyService, "requireUppercase", true);
        ReflectionTestUtils.setField(passwordPolicyService, "requireLowercase", true);
        ReflectionTestUtils.setField(passwordPolicyService, "requireNumbers", true);
        ReflectionTestUtils.setField(passwordPolicyService, "requireSpecialChars", true);
    }

    @Test
    void validatePassword_ShouldReturnValid_WhenPasswordMeetsAllRequirements() {
        // Given
        String validPassword = "SecureP@ss123";

        // When
        PasswordValidationResult result = passwordPolicyService.validatePassword(validPassword);

        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validatePassword_ShouldReturnInvalid_WhenPasswordIsNull() {
        // Given
        String nullPassword = null;

        // When
        PasswordValidationResult result = passwordPolicyService.validatePassword(nullPassword);

        // Then
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Password cannot be empty", result.getErrors().get(0));
    }

    @Test
    void validatePassword_ShouldReturnInvalid_WhenPasswordIsEmpty() {
        // Given
        String emptyPassword = "";

        // When
        PasswordValidationResult result = passwordPolicyService.validatePassword(emptyPassword);

        // Then
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Password cannot be empty", result.getErrors().get(0));
    }

    @Test
    void validatePassword_ShouldReturnInvalid_WhenPasswordTooShort() {
        // Given
        String shortPassword = "Pass1@";

        // When
        PasswordValidationResult result = passwordPolicyService.validatePassword(shortPassword);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must be at least 8 characters long"));
    }

    @Test
    void validatePassword_ShouldReturnInvalid_WhenNoUppercase() {
        // Given
        String noUppercasePassword = "password123@";

        // When
        PasswordValidationResult result = passwordPolicyService.validatePassword(noUppercasePassword);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one uppercase letter"));
    }

    @Test
    void validatePassword_ShouldReturnInvalid_WhenNoLowercase() {
        // Given
        String noLowercasePassword = "PASSWORD123@";

        // When
        PasswordValidationResult result = passwordPolicyService.validatePassword(noLowercasePassword);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one lowercase letter"));
    }

    @Test
    void validatePassword_ShouldReturnInvalid_WhenNoNumbers() {
        // Given
        String noNumbersPassword = "Password@";

        // When
        PasswordValidationResult result = passwordPolicyService.validatePassword(noNumbersPassword);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one number"));
    }

    @Test
    void validatePassword_ShouldReturnInvalid_WhenNoSpecialChars() {
        // Given
        String noSpecialCharsPassword = "Password123";

        // When
        PasswordValidationResult result = passwordPolicyService.validatePassword(noSpecialCharsPassword);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one special character"));
    }

    @Test
    void validatePassword_ShouldReturnInvalid_WhenCommonPassword() {
        // Given
        String commonPassword = "Password123";

        // When
        PasswordValidationResult result = passwordPolicyService.validatePassword(commonPassword);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password is too common, please choose a stronger password"));
    }

    @Test
    void validatePassword_ShouldReturnMultipleErrors_WhenMultipleViolations() {
        // Given
        String weakPassword = "pass"; // Too short, no uppercase, no numbers, no special chars

        // When
        PasswordValidationResult result = passwordPolicyService.validatePassword(weakPassword);

        // Then
        assertFalse(result.isValid());
        assertEquals(4, result.getErrors().size());
        assertTrue(result.getErrors().contains("Password must be at least 8 characters long"));
        assertTrue(result.getErrors().contains("Password must contain at least one uppercase letter"));
        assertTrue(result.getErrors().contains("Password must contain at least one number"));
        assertTrue(result.getErrors().contains("Password must contain at least one special character"));
    }

    @Test
    void calculatePasswordStrength_ShouldReturnVeryWeak_WhenPasswordIsNull() {
        // Given
        String nullPassword = null;

        // When
        PasswordStrength strength = passwordPolicyService.calculatePasswordStrength(nullPassword);

        // Then
        assertEquals(PasswordStrength.VERY_WEAK, strength);
    }

    @Test
    void calculatePasswordStrength_ShouldReturnVeryWeak_WhenPasswordIsEmpty() {
        // Given
        String emptyPassword = "";

        // When
        PasswordStrength strength = passwordPolicyService.calculatePasswordStrength(emptyPassword);

        // Then
        assertEquals(PasswordStrength.VERY_WEAK, strength);
    }

    @Test
    void calculatePasswordStrength_ShouldReturnVeryWeak_WhenScoreIsLow() {
        // Given
        String weakPassword = "pass"; // Only lowercase, less than 8 chars

        // When
        PasswordStrength strength = passwordPolicyService.calculatePasswordStrength(weakPassword);

        // Then
        assertEquals(PasswordStrength.VERY_WEAK, strength);
    }

    @Test
    void calculatePasswordStrength_ShouldReturnWeak_WhenScoreIsTwo() {
        // Given
        String weakPassword = "password"; // 8+ chars, lowercase only

        // When
        PasswordStrength strength = passwordPolicyService.calculatePasswordStrength(weakPassword);

        // Then
        assertEquals(PasswordStrength.WEAK, strength);
    }

    @Test
    void calculatePasswordStrength_ShouldReturnFair_WhenScoreIsThree() {
        // Given
        String fairPassword = "Password"; // 8+ chars, lowercase, uppercase

        // When
        PasswordStrength strength = passwordPolicyService.calculatePasswordStrength(fairPassword);

        // Then
        assertEquals(PasswordStrength.FAIR, strength);
    }

    @Test
    void calculatePasswordStrength_ShouldReturnGood_WhenScoreIsFour() {
        // Given
        String goodPassword = "Password1"; // 8+ chars, lowercase, uppercase, numbers

        // When
        PasswordStrength strength = passwordPolicyService.calculatePasswordStrength(goodPassword);

        // Then
        assertEquals(PasswordStrength.GOOD, strength);
    }

    @Test
    void calculatePasswordStrength_ShouldReturnStrong_WhenScoreIsFive() {
        // Given
        String strongPassword = "Password1@"; // 8+ chars, lowercase, uppercase, numbers, special chars

        // When
        PasswordStrength strength = passwordPolicyService.calculatePasswordStrength(strongPassword);

        // Then
        assertEquals(PasswordStrength.STRONG, strength);
    }

    @Test
    void calculatePasswordStrength_ShouldReturnVeryStrong_WhenScoreIsSix() {
        // Given
        String veryStrongPassword = "VerySecurePassword1@"; // 12+ chars, all requirements

        // When
        PasswordStrength strength = passwordPolicyService.calculatePasswordStrength(veryStrongPassword);

        // Then
        assertEquals(PasswordStrength.VERY_STRONG, strength);
    }

    @Test
    void validatePassword_ShouldWork_WithDifferentSpecialCharacters() {
        // Given - Using non-common password patterns
        String[] passwordsWithSpecialChars = {
            "SecureKey1!", "MySecret2@", "StrongCode3#", "SafeData4$", 
            "CryptoHash5%", "TokenAuth6^", "SecretKey7&", "PrivateData8*"
        };

        // When & Then
        for (String password : passwordsWithSpecialChars) {
            PasswordValidationResult result = passwordPolicyService.validatePassword(password);
            assertTrue(result.isValid(), "Password with special char should be valid: " + password + 
                      ". Errors: " + result.getErrors());
        }
    }

    @Test
    void validatePassword_ShouldDetectCommonPatterns() {
        // Given
        String[] commonPasswords = {
            "password123", "Password123", "123456789", "qwerty123", 
            "admin123", "welcome123", "letmein123"
        };

        // When & Then
        for (String password : commonPasswords) {
            PasswordValidationResult result = passwordPolicyService.validatePassword(password);
            assertFalse(result.isValid(), "Common password should be invalid: " + password);
            assertTrue(result.getErrors().contains("Password is too common, please choose a stronger password"));
        }
    }
}