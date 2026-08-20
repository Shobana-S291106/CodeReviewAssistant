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
        String[] lines = code.split("\\R");

        int totalLines = lines.length;
        int classes = 0;
        int methods = 0;
        int comments = 0;
        int todos = 0;

        List<String> issueMessages = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i];
            String trimmed = line.trim();

            // Classes
            if (trimmed.contains(" class ")) {
                classes++;
            }

            // Methods
            if (isMethodLine(trimmed)) {
                methods++;
            }

            // Comments
            if (trimmed.startsWith("//")
                    || trimmed.startsWith("/*")
                    || trimmed.startsWith("*")) {
                comments++;
            }

            // TODO
            if (line.contains("TODO")) {
                todos++;

                issueMessages.add(
                        "Line " + (i + 1) + ": TODO comment found."
                );
            }

            // System.out.println
            if (line.contains("System.out.println")) {

                issueMessages.add(
                        "Line " + (i + 1)
                                + ": Avoid using System.out.println() in production code."
                );
            }

            // Generic exception
            if (trimmed.contains("catch (Exception")
                    || trimmed.contains("catch(Exception")) {

                issueMessages.add(
                        "Line " + (i + 1)
                                + ": Generic exception caught. Catch specific exceptions instead."
                );
            }

            // Null check
            if (line.contains("== null")
                    || line.contains("!= null")) {

                issueMessages.add(
                        "Line " + (i + 1)
                                + ": Null check detected. Consider using a null-safe approach where appropriate."
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
                report.append(issue).append("\n");
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

        String[] lines = code.split("\\R");

        List<ReviewIssue> issues = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i];
            String trimmed = line.trim();

            // Ignore normal comments when checking code patterns
            boolean isComment =
                    trimmed.startsWith("//")
                            || trimmed.startsWith("/*")
                            || trimmed.startsWith("*");


            // =================================================
            // System.out.println
            // =================================================

            if (line.contains("System.out.println")) {

                issues.add(new ReviewIssue(
                        "MEDIUM",
                        "CODE_SMELL",
                        "System.out.println() is used.",
                        i + 1,
                        "Use a proper logging framework instead of System.out.println()."
                ));
            }


            // =================================================
            // TODO
            // =================================================

            if (line.contains("TODO")) {

                issues.add(new ReviewIssue(
                        "LOW",
                        "MAINTENANCE",
                        "TODO comment detected.",
                        i + 1,
                        "Review and complete the unfinished implementation."
                ));
            }


            // =================================================
            // Generic Exception
            // =================================================

            if (trimmed.contains("catch (Exception")
                    || trimmed.contains("catch(Exception")) {

                issues.add(new ReviewIssue(
                        "MEDIUM",
                        "ERROR_HANDLING",
                        "Generic Exception is caught.",
                        i + 1,
                        "Catch a specific exception type instead of using Exception."
                ));
            }


            // =================================================
            // Null check
            // =================================================

            if (!isComment
                    && (line.contains("== null")
                    || line.contains("!= null"))) {

                issues.add(new ReviewIssue(
                        "LOW",
                        "NULL_SAFETY",
                        "Null check detected.",
                        i + 1,
                        "Consider a null-safe approach such as Optional where appropriate."
                ));
            }


            // =================================================
            // String comparison
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
                            i + 1,
                            "Use .equals() or .equalsIgnoreCase() to compare String values."
                    ));
                }
            }


            // =================================================
            // Hard-coded password
            // =================================================

            String lower = line.toLowerCase();

            if (!isComment && lower.contains("password")) {

                issues.add(new ReviewIssue(
                        "HIGH",
                        "SECURITY",
                        "Possible password or credential detected.",
                        i + 1,
                        "Avoid hard-coded credentials. Use environment variables or a secure secret manager."
                ));
            }


            // =================================================
            // Long line
            // =================================================

            if (line.length() > 120) {

                issues.add(new ReviewIssue(
                        "LOW",
                        "CODE_SMELL",
                        "Line exceeds 120 characters.",
                        i + 1,
                        "Break the statement into smaller and more readable lines."
                ));
            }
        }

        return issues;
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
                ".*\\b(public|private|protected)\\b.*\\([^;]*\\)\\s*\\{?"
        );
    }
}