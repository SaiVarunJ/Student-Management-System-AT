package com.airtripe.studentmanagement.util;

import java.util.regex.Pattern;

public class InputValidator {
    private static final Pattern EMAIL = Pattern.compile("^[^@\s]+@[^@\s]+\\.[^@\s]+$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }

    public static boolean notNullOrEmpty(String s) { return s != null && !s.isBlank(); }
}
