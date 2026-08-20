package com.codereviewassistant.backend.service;

import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    public GeminiService() {
    }

    public String reviewCode(String code) {

        StringBuilder report = new StringBuilder();

        report.append("===== Smart Code Review =====\n\n");

        // 1. Empty code
        if (code == null || code.trim().isEmpty()) {
            report.append("No code was provided.\n");
            return report.toString();
        }

        // 2. System.out.println
        if (code.contains("System.out.println")) {
            report.append("• Code Smell: System.out.println() is used. ");
            report.append("Consider using a proper logging framework.\n");
        }

        // 3. Empty catch block
        if (code.matches("(?s).*catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}.*")) {
            report.append("• Potential Issue: Empty catch block detected. ");
            report.append("Exceptions should be handled or logged.\n");
        }

        // 4. String comparison using ==
        if (code.matches("(?s).*String\\s+\\w+.*==.*String.*")) {
            report.append("• Potential Logic Bug: Check String comparisons. ");
            report.append("Use .equals() instead of == for String values.\n");
        }

        // 5. Hard-coded password
        if (code.toLowerCase().contains("password")) {
            report.append("• Security Warning: Code contains a variable or text ");
            report.append("related to 'password'. Avoid hard-coded credentials.\n");
        }

        // 6. TODO
        if (code.contains("TODO")) {
            report.append("• Code Maintenance: TODO comment detected. ");
            report.append("Review unfinished implementation.\n");
        }

        // 7. Very long lines
        String[] lines = code.split("\\R");

        int longLines = 0;

        for (String line : lines) {
            if (line.length() > 120) {
                longLines++;
            }
        }

        if (longLines > 0) {
            report.append("• Code Smell: ")
                  .append(longLines)
                  .append(" line(s) exceed 120 characters.\n");
        }

        // 8. Nested loops
        int loopCount = 0;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("for ") ||
                trimmed.startsWith("for(") ||
                trimmed.startsWith("for (") ||
                trimmed.startsWith("while ") ||
                trimmed.startsWith("while(") ||
                trimmed.startsWith("while (")) {

                loopCount++;
            }
        }

        if (loopCount >= 2) {
            report.append("• Performance Warning: Multiple loops detected. ");
            report.append("Check whether nested loops can be optimized.\n");
        }

        // No issues
        if (report.toString().equals("===== Smart Code Review =====\n\n")) {
            report.append("• No obvious issues detected by the current static rules.\n");
        }

        report.append("\n===== Recommendation =====\n");
        report.append("Review the detected issues and verify the code's logic and edge cases.");

        return report.toString();
    }
}