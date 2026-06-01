package it.booking.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final String ALLOWED_SPECIALS = "!@#$%^&*()_+-=[]{}:,.?";
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 16;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return false;
        }

        boolean hasLowercase = false;
        boolean hasUppercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char ch : value.toCharArray()) {
            if (Character.isLowerCase(ch)) {
                hasLowercase = true;
            } else if (Character.isUpperCase(ch)) {
                hasUppercase = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (ALLOWED_SPECIALS.indexOf(ch) >= 0) {
                hasSpecial = true;
            } else {
                return false;
            }
        }

        return hasLowercase && hasUppercase && hasDigit && hasSpecial;
    }
}
