package com.codereviewassistant.backend.service;

import com.codereviewassistant.backend.dto.ReviewIssue;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class CodeAnalysisService {

    // =========================================================
    // LOCAL TEXT REPORT
    // =========================================================

    public String analyze(File file) throws Exception {

        String code = Files.readString(file.toPath());

        String[] lines = code.split("\\R", -1);

        int totalLines = lines.length;
        int classes = 0;
        int methods = 0;
        int comments = 0;
        int todos = 0;

        List<String> issueMessages = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i];

            String trimmed = line.trim();

            // =================================================
            // CLASSES
            // =================================================

            if (trimmed.contains(" class ")
                    || trimmed.startsWith("class ")
                    || trimmed.contains(" class{")) {

                classes++;
            }

            // =================================================
            // METHODS
            // =================================================

            if (isMethodLine(trimmed)) {
                methods++;
            }

            // =================================================
            // COMMENTS
            // =================================================

            if (trimmed.startsWith("//")
                    || trimmed.startsWith("/*")
                    || trimmed.startsWith("*")) {

                comments++;
            }

            // =================================================
            // TODO
            // =================================================

            if (line.contains("TODO")
                    || line.contains("FIXME")) {

                todos++;

                issueMessages.add(
                        "Line " + (i + 1)
                                + ": TODO or FIXME comment found."
                );
            }

            // =================================================
            // SYSTEM.OUT
            // =================================================

            if (line.contains("System.out.println")
                    || line.contains("System.out.print(")
                    || line.contains("System.out.printf(")) {

                issueMessages.add(
                        "Line " + (i + 1)
                                + ": Avoid using System.out in production code."
                );
            }

            // =================================================
            // GENERIC EXCEPTION
            // =================================================

            if (trimmed.contains("catch (Exception")
                    || trimmed.contains("catch(Exception")) {

                issueMessages.add(
                        "Line " + (i + 1)
                                + ": Generic exception caught. "
                                + "Catch specific exceptions instead."
                );
            }
        }

        StringBuilder report = new StringBuilder();

        report.append("===== Code Analysis Report =====\n\n");

        report.append("Total Lines : ")
                .append(totalLines)
                .append("\n");

        report.append("Classes     : ")
                .append(classes)
                .append("\n");

        report.append("Methods     : ")
                .append(methods)
                .append("\n");

        report.append("Comments    : ")
                .append(comments)
                .append("\n");

        report.append("TODOs       : ")
                .append(todos)
                .append("\n\n");

        report.append("===== Issues Found =====\n");

        if (issueMessages.isEmpty()) {

            report.append("No obvious issues detected.\n");

        } else {

            for (String issue : issueMessages) {

                report.append(issue)
                        .append("\n");
            }
        }

        return report.toString();
    }


    // =========================================================
    // STRUCTURED LOCAL ISSUES
    // =========================================================

    public List<ReviewIssue> getStructuredIssues(File file)
            throws Exception {

        String code = Files.readString(file.toPath());

        String[] lines = code.split("\\R", -1);

        List<ReviewIssue> issues = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i];

            String trimmed = line.trim();

            int lineNumber = i + 1;

            // =================================================
            // COMMENT DETECTION
            // =================================================

            boolean isComment =
                    trimmed.startsWith("//")
                            || trimmed.startsWith("/*")
                            || trimmed.startsWith("*");


            // =================================================
            // SYSTEM.OUT
            // =================================================

            if (!isComment
                    && (line.contains("System.out.println")
                    || line.contains("System.out.print(")
                    || line.contains("System.out.printf("))) {

                issues.add(new ReviewIssue(
                        "MEDIUM",
                        "CODE_SMELL",
                        "System.out is used in production code.",
                        lineNumber,
                        "Use a proper logging framework such as SLF4J."
                ));
            }


            // =================================================
            // TODO / FIXME
            // =================================================

            if (isComment
                    && (line.contains("TODO")
                    || line.contains("FIXME"))) {

                issues.add(new ReviewIssue(
                        "LOW",
                        "MAINTENANCE",
                        "TODO or FIXME comment detected.",
                        lineNumber,
                        "Review and complete the unfinished implementation."
                ));
            }


            // =================================================
            // GENERIC EXCEPTION
            // =================================================

            if (!isComment
                    && (trimmed.contains("catch (Exception")
                    || trimmed.contains("catch(Exception"))) {

                issues.add(new ReviewIssue(
                        "MEDIUM",
                        "ERROR_HANDLING",
                        "Generic Exception is caught.",
                        lineNumber,
                        "Catch a specific exception type instead of using Exception."
                ));
            }


            // =================================================
            // STRING COMPARISON
            // =================================================

            if (!isComment) {

                boolean stringComparison =
                        line.matches(
                                ".*\\b\\w+\\s*==\\s*\".*\".*"
                        )
                        ||
                        line.matches(
                                ".*\".*\"\\s*==\\s*\\b\\w+.*"
                        )
                        ||
                        line.matches(
                                ".*\\b\\w+\\s*!=\\s*\".*\".*"
                        )
                        ||
                        line.matches(
                                ".*\".*\"\\s*!=\\s*\\b\\w+.*"
                        );

                if (stringComparison) {

                    issues.add(new ReviewIssue(
                            "HIGH",
                            "LOGIC_BUG",
                            "Possible String comparison using == or !=.",
                            lineNumber,
                            "Use .equals() or .equalsIgnoreCase() to compare String values."
                    ));
                }
            }


            // =================================================
            // HARD-CODED CREDENTIAL
            // =================================================

            if (!isComment) {

                String lower = line.toLowerCase();

                boolean credentialDetected =
                        lower.contains("password")
                                || lower.contains("passwd")
                                || lower.contains("api_key")
                                || lower.contains("apikey")
                                || lower.contains("secret")
                                || lower.contains("token");

                if (credentialDetected
                        && lower.contains("=")
                        && lower.contains("\"")) {

                    issues.add(new ReviewIssue(
                            "HIGH",
                            "SECURITY",
                            "Possible hard-coded credential detected.",
                            lineNumber,
                            "Avoid hard-coded credentials. "
                                    + "Use environment variables or a secure secret manager."
                    ));
                }
            }


            // =================================================
            // LONG LINE
            // =================================================

            if (line.length() > 120) {

                issues.add(new ReviewIssue(
                        "LOW",
                        "CODE_SMELL",
                        "Line exceeds 120 characters.",
                        lineNumber,
                        "Break the statement into smaller and more readable lines."
                ));
            }
        }

        return issues;
    }


    // =========================================================
    // SMART REVIEW
    // =========================================================

    public String generateSmartReview(
            List<ReviewIssue> issues
    ) {

        StringBuilder smartReview =
                new StringBuilder();

        smartReview.append(
                "===== Smart Code Review =====\n\n"
        );

        // =====================================================
        // NO ISSUES
        // =====================================================

        if (issues == null || issues.isEmpty()) {

            smartReview.append(
                    "• No obvious issues detected by the current static rules.\n\n"
            );

            smartReview.append(
                    "===== Recommendation =====\n"
            );

            smartReview.append(
                    "Your code passed the current analysis rules. "
                            + "Continue reviewing business logic and edge cases.\n"
            );

            return smartReview.toString();
        }


        // =====================================================
        // GROUP ISSUE TYPES
        // =====================================================

        boolean logicBug = false;
        boolean security = false;
        boolean codeSmell = false;
        boolean maintenance = false;
        boolean errorHandling = false;

        for (ReviewIssue issue : issues) {

            if (issue.getCategory() == null) {
                continue;
            }

            String category =
                    issue.getCategory().toUpperCase();

            switch (category) {

                case "LOGIC_BUG":
                    logicBug = true;
                    break;

                case "SECURITY":
                    security = true;
                    break;

                case "CODE_SMELL":
                    codeSmell = true;
                    break;

                case "MAINTENANCE":
                    maintenance = true;
                    break;

                case "ERROR_HANDLING":
                    errorHandling = true;
                    break;

                default:
                    break;
            }
        }


        // =====================================================
        // SMART SUMMARY
        // =====================================================

        if (logicBug) {

            smartReview.append(
                    "• Logic Warning: Potential logic bugs "
                            + "were detected. Review conditions, "
                            + "comparisons and edge cases.\n"
            );
        }

        if (security) {

            smartReview.append(
                    "• Security Warning: Potential hard-coded "
                            + "credentials or sensitive information "
                            + "were detected.\n"
            );
        }

        if (codeSmell) {

            smartReview.append(
                    "• Code Smell: Code-quality issues were detected. "
                            + "Consider improving logging and readability.\n"
            );
        }

        if (maintenance) {

            smartReview.append(
                    "• Code Maintenance: TODO or FIXME comments "
                            + "were detected.\n"
            );
        }

        if (errorHandling) {

            smartReview.append(
                    "• Error Handling Warning: Exception handling "
                            + "could be improved.\n"
            );
        }


        // =====================================================
        // RECOMMENDATION
        // =====================================================

        smartReview.append("\n");

        smartReview.append(
                "===== Recommendation =====\n"
        );

        smartReview.append(
                "Review the detected issues and verify the "
                        + "code's logic, security and edge cases.\n"
        );

        return smartReview.toString();
    }


    // =========================================================
    // METHOD DETECTION
    // =========================================================

    private boolean isMethodLine(String line) {

        if (line.startsWith("//")
                || line.startsWith("*")
                || line.startsWith("/*")) {

            return false;
        }

        return line.matches(
                ".*\\b(public|private|protected)\\b"
                        + ".*\\([^;]*\\)\\s*\\{?"
        );
    }
}