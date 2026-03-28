package com.vibes.iam.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordPolicyService {

    @Value("${app.security.password.min-length}")
    private int minLength;

    @Value("${app.security.password.require-uppercase}")
    private boolean requireUppercase;

    @Value("${app.security.password.require-lowercase}")
    private boolean requireLowercase;

    @Value("${app.security.password.require-numbers}")
    private boolean requireNumbers;

    @Value("${app.security.password.require-special-chars}")
    private boolean requireSpecialChars;

    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String NUMBERS_PATTERN = ".*[0-9].*";
    private static final String SPECIAL_CHARS_PATTERN = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*";

    public PasswordValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        boolean isValid = true;

        if (password == null || password.isEmpty()) {
            errors.add("Password cannot be empty");
            return new PasswordValidationResult(false, errors);
        }

        if (password.length() < minLength) {
            errors.add("Password must be at least " + minLength + " characters long");
            isValid = false;
        }

        if (requireUppercase && !Pattern.matches(UPPERCASE_PATTERN, password)) {
            errors.add("Password must contain at least one uppercase letter");
            isValid = false;
        }

        if (requireLowercase && !Pattern.matches(LOWERCASE_PATTERN, password)) {
            errors.add("Password must contain at least one lowercase letter");
            isValid = false;
        }

        if (requireNumbers && !Pattern.matches(NUMBERS_PATTERN, password)) {
            errors.add("Password must contain at least one number");
            isValid = false;
        }

        if (requireSpecialChars && !Pattern.matches(SPECIAL_CHARS_PATTERN, password)) {
            errors.add("Password must contain at least one special character");
            isValid = false;
        }

        if (containsCommonPasswords(password)) {
            errors.add("Password is too common, please choose a stronger password");
            isValid = false;
        }

        return new PasswordValidationResult(isValid, errors);
    }

    public PasswordStrength calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.VERY_WEAK;
        }

        int score = 0;

        if (password.length() >= 8) score += 1;
        if (password.length() >= 12) score += 1;
        if (Pattern.matches(LOWERCASE_PATTERN, password)) score += 1;
        if (Pattern.matches(UPPERCASE_PATTERN, password)) score += 1;
        if (Pattern.matches(NUMBERS_PATTERN, password)) score += 1;
        if (Pattern.matches(SPECIAL_CHARS_PATTERN, password)) score += 1;

        if (score <= 1) return PasswordStrength.VERY_WEAK;
        if (score <= 2) return PasswordStrength.WEAK;
        if (score <= 3) return PasswordStrength.FAIR;
        if (score <= 4) return PasswordStrength.GOOD;
        if (score <= 5) return PasswordStrength.STRONG;
        return PasswordStrength.VERY_STRONG;
    }

    private boolean containsCommonPasswords(String password) {
        String[] commonPasswords = {
            "password", "123456", "password123", "admin", "qwerty", 
            "letmein", "welcome", "monkey", "1234567890", "abc123",
            "Password1", "password1", "123456789", "welcome123"
        };

        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.contains(common.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public PasswordValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public enum PasswordStrength {
        VERY_WEAK, WEAK, FAIR, GOOD, STRONG, VERY_STRONG
    }
}