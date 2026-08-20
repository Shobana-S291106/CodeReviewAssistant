package com.codereviewassistant.backend.analyzer;

import com.codereviewassistant.backend.dto.AnalysisResult;
import com.codereviewassistant.backend.dto.ReviewIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JavaCodeAnalyzer {

    // =========================================================
    // REGEX PATTERNS
    // =========================================================

    private static final Pattern STRING_COMPARISON =
            Pattern.compile(
                    "(\\b\\w+\\b\\s*(==|!=)\\s*\"[^\"]*\"|"
                            + "\"[^\"]*\"\\s*(==|!=)\\s*\\b\\w+\\b)"
            );

    private static final Pattern HARD_CODED_SECRET =
            Pattern.compile(
                    "(?i)\\b(password|passwd|api[_-]?key|apikey|secret|token)\\b"
                            + "\\s*=\\s*"
                            + "(\"[^\"]*\"|\\d+)"
            );

    private static final Pattern SYSTEM_OUT =
            Pattern.compile(
                    "\\bSystem\\.out\\.(println|print|printf)\\s*\\("
            );

    private static final Pattern TODO_PATTERN =
            Pattern.compile("(?i)\\b(TODO|FIXME)\\b");

    private static final Pattern CATCH_PATTERN =
            Pattern.compile("\\bcatch\\s*\\([^)]*\\)");

    private static final Pattern LOOP_PATTERN =
            Pattern.compile("^\\s*(for|while|do)\\b");

    private static final Pattern HTTP_URL =
            Pattern.compile("\"http://[^\"]+\"");

    private static final Pattern DIVISION_BY_ZERO =
            Pattern.compile("/\\s*0\\b");

    private static final Pattern DIVISION_BY_VARIABLE =
            Pattern.compile(
                    "/\\s*([a-zA-Z_$][a-zA-Z0-9_$]*)"
            );

    private static final Pattern ARRAY_ACCESS =
            Pattern.compile(
                    "\\b([a-zA-Z_$][a-zA-Z0-9_$]*)"
                            + "\\s*\\[\\s*([^\\]]+)\\s*\\]"
            );

    private static final Pattern RETURN_PATTERN =
            Pattern.compile("^\\s*return\\b");


    // =========================================================
    // MAIN ANALYZER
    // =========================================================

    public AnalysisResult analyze(String code) {

        List<ReviewIssue> issues =
                new ArrayList<>();

        // =====================================================
        // EMPTY FILE
        // =====================================================

        if (code == null || code.trim().isEmpty()) {

            issues.add(
                    new ReviewIssue(
                            "HIGH",
                            "VALIDATION",
                            "Uploaded file is empty.",
                            0,
                            "Upload a Java file containing source code."
                    )
            );

            return new AnalysisResult(
                    "Analysis completed with 1 issue.",
                    80,
                    issues
            );
        }

        String[] lines =
                code.split("\\R", -1);

        int score = 100;

        int loopCount = 0;

        boolean insideBlockComment = false;


        // =====================================================
        // LINE-BY-LINE ANALYSIS
        // =====================================================

        for (int i = 0; i < lines.length; i++) {

            String line =
                    lines[i];

            String trimmed =
                    line.trim();

            int lineNumber =
                    i + 1;


            if (trimmed.isEmpty()) {
                continue;
            }


            // =================================================
            // COMMENT DETECTION
            // =================================================

            if (trimmed.startsWith("/*")) {
                insideBlockComment = true;
            }

            boolean isComment =
                    insideBlockComment
                            || trimmed.startsWith("//")
                            || trimmed.startsWith("*");

            if (trimmed.contains("*/")) {
                insideBlockComment = false;
            }


            // =================================================
            // TODO / FIXME
            // =================================================

            if (isComment
                    && TODO_PATTERN.matcher(line).find()) {

                addIssue(
                        issues,
                        "LOW",
                        "MAINTENANCE",
                        "TODO or FIXME comment detected.",
                        lineNumber,
                        "Review and complete the unfinished implementation."
                );

                score -= 2;
            }


            // Ignore comments for code rules
            if (isComment) {
                continue;
            }


            // =================================================
            // 1. SYSTEM.OUT
            // =================================================

            if (SYSTEM_OUT.matcher(line).find()) {

                addIssue(
                        issues,
                        "MEDIUM",
                        "CODE_SMELL",
                        "Avoid using System.out.println() in production code.",
                        lineNumber,
                        "Use a proper logging framework such as SLF4J."
                );

                score -= 5;
            }


            // =================================================
            // 2. STRING COMPARISON
            // =================================================

            if (STRING_COMPARISON.matcher(line).find()) {

                addIssue(
                        issues,
                        "HIGH",
                        "LOGIC_BUG",
                        "Possible String comparison using == or !=.",
                        lineNumber,
                        "Use .equals() or .equalsIgnoreCase() to compare String values."
                );

                score -= 15;
            }


            // =================================================
            // 3. HARD-CODED SECRET
            // =================================================

            if (HARD_CODED_SECRET.matcher(line).find()) {

                addIssue(
                        issues,
                        "HIGH",
                        "SECURITY",
                        "Possible hard-coded credential detected.",
                        lineNumber,
                        "Use environment variables or a secure secret manager."
                );

                score -= 20;
            }


            // =================================================
            // 4. GENERIC EXCEPTION
            // =================================================

            if (isGenericExceptionCatch(line)) {

                addIssue(
                        issues,
                        "MEDIUM",
                        "ERROR_HANDLING",
                        "Generic Exception is caught.",
                        lineNumber,
                        "Catch a specific exception type instead of using Exception."
                );

                score -= 5;
            }


            // =================================================
            // 5. EMPTY CATCH
            // =================================================

            if (CATCH_PATTERN.matcher(line).find()
                    && isEmptyCatch(lines, i)) {

                addIssue(
                        issues,
                        "HIGH",
                        "ERROR_HANDLING",
                        "Possible empty catch block.",
                        lineNumber,
                        "Handle the exception properly or log the error."
                );

                score -= 15;
            }


            // =================================================
            // 6. LONG LINE
            // =================================================

            if (line.length() > 120) {

                addIssue(
                        issues,
                        "LOW",
                        "CODE_SMELL",
                        "Line exceeds 120 characters.",
                        lineNumber,
                        "Break the statement into smaller and more readable lines."
                );

                score -= 1;
            }


            // =================================================
            // 7. LOOP DETECTION
            // =================================================

            if (LOOP_PATTERN.matcher(trimmed).find()) {

                loopCount++;
            }


            // =================================================
            // 8. DEFINITE DIVISION BY ZERO
            // =================================================

            if (DIVISION_BY_ZERO.matcher(line).find()) {

                addIssue(
                        issues,
                        "HIGH",
                        "LOGIC_BUG",
                        "Definite division by zero detected.",
                        lineNumber,
                        "Do not divide by zero. Validate the divisor before performing the division."
                );

                score -= 20;
            }


            // =================================================
            // 9. DIVISION BY VARIABLE
            // =================================================

            Matcher divisionMatcher =
                    DIVISION_BY_VARIABLE.matcher(line);

            if (divisionMatcher.find()) {

                String divisor =
                        divisionMatcher.group(1);


                // Ignore common non-variable cases
                if (!divisor.equals("this")
                        && !divisor.equals("super")
                        && !divisor.equals("1")) {

                    // Do not create duplicate issue for /0
                    if (!DIVISION_BY_ZERO.matcher(line).find()) {

                        if (!hasZeroCheck(
                                lines,
                                i,
                                divisor)) {

                            addIssue(
                                    issues,
                                    "MEDIUM",
                                    "LOGIC_BUG",
                                    "Possible division by zero risk for variable '"
                                            + divisor
                                            + "'.",
                                    lineNumber,
                                    "Ensure the divisor is checked for zero before division."
                            );

                            score -= 10;
                        }
                    }
                }
            }


            // =================================================
            // 10. ARRAY ACCESS
            // =================================================

            Matcher arrayMatcher =
                    ARRAY_ACCESS.matcher(line);

            if (arrayMatcher.find()) {

                String arrayName =
                        arrayMatcher.group(1);

                String index =
                        arrayMatcher.group(2).trim();


                // Ignore simple numeric indexes
                if (!index.matches("\\d+")
                        && !hasBoundsCheck(
                        lines,
                        i,
                        arrayName,
                        index)) {

                    addIssue(
                            issues,
                            "MEDIUM",
                            "LOGIC_BUG",
                            "Array access may cause an IndexOutOfBoundsException.",
                            lineNumber,
                            "Validate the index against the array length before accessing the array."
                    );

                    score -= 8;
                }
            }


            // =================================================
            // 11. HTTP URL
            // =================================================

            if (HTTP_URL.matcher(line).find()) {

                addIssue(
                        issues,
                        "LOW",
                        "CONFIGURATION",
                        "Hard-coded HTTP URL detected.",
                        lineNumber,
                        "Move environment-specific URLs into configuration."
                );

                score -= 2;
            }


            // =================================================
            // 12. POSSIBLE UNREACHABLE CODE
            // =================================================

            if (i > 0) {

                String previous =
                        lines[i - 1].trim();


                if (RETURN_PATTERN.matcher(previous).find()
                        && !trimmed.equals("}")
                        && !trimmed.startsWith("else")
                        && !trimmed.startsWith("catch")
                        && !trimmed.startsWith("//")) {

                    addIssue(
                            issues,
                            "HIGH",
                            "LOGIC_BUG",
                            "Possible unreachable code detected.",
                            lineNumber,
                            "Remove the unreachable statement or move it before the return."
                    );

                    score -= 12;
                }
            }
        }


        // =====================================================
        // 13. MULTIPLE LOOPS
        // =====================================================

        if (loopCount >= 3) {

            addIssue(
                    issues,
                    "LOW",
                    "PERFORMANCE",
                    "Multiple loops detected.",
                    0,
                    "Review repeated or nested loops for unnecessary work."
            );

            score -= 3;
        }


        // =====================================================
        // SCORE LIMIT
        // =====================================================

        score =
                Math.max(
                        0,
                        Math.min(100, score)
                );


        // =====================================================
        // REPORT
        // =====================================================

        String report;

        if (issues.isEmpty()) {

            report =
                    "No major issues found.";

        } else {

            report =
                    "Analysis completed with "
                            + issues.size()
                            + " issue(s).";
        }


        // =====================================================
        // FINAL RESULT
        // =====================================================

        return new AnalysisResult(
                report,
                score,
                issues
        );
    }


    // =========================================================
    // ADD ISSUE
    // =========================================================

    private void addIssue(
            List<ReviewIssue> issues,
            String severity,
            String category,
            String message,
            int line,
            String suggestion
    ) {

        issues.add(
                new ReviewIssue(
                        severity,
                        category,
                        message,
                        line,
                        suggestion
                )
        );
    }


    // =========================================================
    // GENERIC EXCEPTION
    // =========================================================

    private boolean isGenericExceptionCatch(
            String line) {

        String normalized =
                line.replaceAll("\\s+", "");

        return normalized.contains("catch(Exception")
                || normalized.contains(
                "catch(java.lang.Exception"
        );
    }


    // =========================================================
    // EMPTY CATCH
    // =========================================================

    private boolean isEmptyCatch(
            String[] lines,
            int catchLine) {

        for (
                int i = catchLine;
                i < Math.min(lines.length, catchLine + 5);
                i++
        ) {

            String current =
                    lines[i].trim();


            // Example:
            // catch (Exception e) {
            if (i == catchLine
                    && current.contains("{")
                    && current.endsWith("{")) {

                continue;
            }


            // Example:
            // {
            if (current.equals("{")) {

                continue;
            }


            // Example:
            // }
            if (current.equals("}")) {

                return true;
            }


            // Example:
            // catch (Exception e) {}
            if (i == catchLine
                    && current.contains("{")
                    && current.contains("}")) {

                int open =
                        current.indexOf("{");

                int close =
                        current.lastIndexOf("}");


                if (close > open) {

                    String inside =
                            current.substring(
                                    open + 1,
                                    close
                            ).trim();

                    return inside.isEmpty();
                }
            }


            // There is actual content
            if (i > catchLine) {

                return false;
            }
        }

        return false;
    }


    // =========================================================
    // ZERO CHECK
    // =========================================================

    private boolean hasZeroCheck(
            String[] lines,
            int currentLine,
            String variable) {

        int start =
                Math.max(
                        0,
                        currentLine - 5
                );

        String escaped =
                Pattern.quote(variable);


        for (
                int i = start;
                i < currentLine;
                i++
        ) {

            String line =
                    lines[i];


            // Example:
            // if (x != 0)
            // if (x > 0)
            if (line.matches(
                    ".*\\b"
                            + escaped
                            + "\\b\\s*(!=|>)\\s*0.*"
            )) {

                return true;
            }


            // Example:
            // if (x == 0)
            if (line.matches(
                    ".*\\b"
                            + escaped
                            + "\\b\\s*==\\s*0.*"
            )) {

                return true;
            }
        }

        return false;
    }


    // =========================================================
    // ARRAY BOUNDS CHECK
    // =========================================================

    private boolean hasBoundsCheck(
            String[] lines,
            int currentLine,
            String arrayName,
            String index) {

        int start =
                Math.max(
                        0,
                        currentLine - 5
                );


        for (
                int i = start;
                i < currentLine;
                i++
        ) {

            String line =
                    lines[i];


            boolean lengthCheck =
                    line.contains(
                            arrayName + ".length"
                    );


            boolean indexCheck =
                    line.contains(index + " <")
                            || line.contains(index + "<")
                            || line.contains(index + " <=")
                            || line.contains(index + "<=");


            if (lengthCheck && indexCheck) {

                return true;
            }
        }

        return false;
    }
}