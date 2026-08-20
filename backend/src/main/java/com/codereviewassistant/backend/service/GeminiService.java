package com.codereviewassistant.backend.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GeminiService {

    private final Client geminiClient;
    private final String model;

    /*
     * =====================================================
     * CONSTRUCTOR
     * =====================================================
     */

    public GeminiService(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.model:}") String model
    ) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key is not configured. " +
                    "Set the GEMINI_API_KEY environment variable."
            );
        }

        if (model == null || model.isBlank()) {
            throw new IllegalStateException(
                    "Gemini model is not configured. " +
                    "Set gemini.model in application.properties."
            );
        }

        this.model = model.trim();

        this.geminiClient = Client.builder()
                .apiKey(apiKey)
                .build();
    }


    /*
     * =====================================================
     * SMART CODE REVIEW
     * =====================================================
     */

    public String reviewCode(String code) {

        if (code == null || code.trim().isEmpty()) {

            return """
                    ===== Smart Code Review =====

                    No source code was provided.
                    """;
        }

        String prompt = """
                You are an expert senior Java software engineer
                performing a professional code review.

                Analyze the following Java source code carefully.

                Identify only issues that are reasonably supported
                by the supplied source code.

                Look specifically for:

                1. Silent logic bugs
                2. Runtime errors
                3. Edge-case failures
                4. Security vulnerabilities
                5. Code smells
                6. Incorrect exception handling
                7. Resource management problems
                8. Performance problems
                9. Maintainability problems
                10. Incorrect assumptions

                IMPORTANT RULES:

                - Do not invent issues.
                - Do not report generic advice as a bug.
                - Only report issues supported by the source code.
                - Pay special attention to bugs that compile successfully
                  but may produce incorrect behavior.
                - Explain why each issue matters.
                - Give a practical recommendation.
                - Do not rewrite the entire program.
                - Do not repeat the same issue multiple times.
                - If an obvious issue is already detected by a local
                  static analyzer, focus on deeper reasoning.
                - Keep the response concise and useful.

                Use exactly this structure:

                ===== Smart Code Review =====

                Issue 1:
                Severity: HIGH/MEDIUM/LOW
                Category:
                Problem:
                Why:
                Recommendation:

                Issue 2:
                Severity: HIGH/MEDIUM/LOW
                Category:
                Problem:
                Why:
                Recommendation:

                Continue only when meaningful issues exist.

                If there are no meaningful additional issues, write:

                No additional smart-review issues detected.

                Finally write:

                ===== Recommendation =====

                Give a short overall recommendation.

                Java Source Code:
                ----------------
                %s
                ----------------
                """.formatted(code);

        return callGemini(
                prompt,
                "===== Smart Code Review ====="
        );
    }


    /*
     * =====================================================
     * EXPLAIN ISSUE
     * =====================================================
     */

    public String explainIssue(
            String issue,
            String code
    ) {

        if (issue == null) {
            issue = "";
        }

        if (code == null) {
            code = "";
        }

        String prompt = """
                You are an expert Java software engineer.

                Explain the detected issue below to a university
                student learning software engineering.

                Detected Issue:
                %s

                Source Code:
                %s

                Use exactly this structure:

                ===== Issue Explanation =====

                Problem:
                Explain exactly what the issue is.

                Why this is a problem:
                Explain the technical reason clearly.

                Why it can happen:
                Explain the underlying Java/programming concept.

                Recommended Fix:
                Give the correct approach.

                Example:
                Give a small Java example showing the correct approach.

                Impact:
                Explain what could happen if the issue is not fixed.

                IMPORTANT:

                - Do not invent an issue.
                - Base the explanation on the supplied issue and code.
                - Do not rewrite unrelated code.
                - Keep the explanation technically accurate.
                """.formatted(issue, code);

        return callGemini(
                prompt,
                "===== Issue Explanation ====="
        );
    }


    /*
     * =====================================================
     * MAP VERSION
     * =====================================================
     */

    public String explainIssue(
            Map<String, String> request
    ) {

        if (request == null) {
            return explainIssue("", "");
        }

        String issue = request.getOrDefault(
                "issue",
                ""
        );

        String code = request.getOrDefault(
                "code",
                ""
        );

        return explainIssue(issue, code);
    }


    /*
     * =====================================================
     * SUGGEST FIX
     * =====================================================
     */

    public String suggestFix(
            String issue,
            String code
    ) {

        if (issue == null) {
            issue = "";
        }

        if (code == null) {
            code = "";
        }

        String prompt = """
                You are an expert Java software engineer.

                The following issue was detected:

                Issue:
                %s

                Source Code:
                %s

                Provide a practical and safe fix.

                Use exactly this structure:

                ===== Suggested Fix =====

                Issue:
                Briefly describe the issue.

                Problematic Code:
                Show only the relevant problematic code.

                Suggested Fix:
                Show the corrected code.

                Why:
                Explain why the corrected version is better.

                Additional Recommendation:
                Mention an important improvement or edge case.

                IMPORTANT:

                - Preserve the original program's intended behavior.
                - Do not rewrite unrelated code.
                - Do not invent APIs or classes.
                - Do not change the programming language.
                - Make the smallest reasonable fix.
                - If the exact fix cannot be determined,
                  clearly explain what additional context is needed.
                """.formatted(issue, code);

        return callGemini(
                prompt,
                "===== Suggested Fix ====="
        );
    }


    /*
     * =====================================================
     * COMMON GEMINI API CALL
     * =====================================================
     */

    private String callGemini(
            String prompt,
            String fallbackHeader
    ) {

        try {

            GenerateContentResponse response =
                    geminiClient.models.generateContent(
                            model,
                            prompt,
                            null
                    );

            if (response == null) {

                return fallbackHeader
                        + "\n\n"
                        + "Gemini returned no response.";
            }

            String result = response.text();

            if (result == null || result.isBlank()) {

                return fallbackHeader
                        + "\n\n"
                        + "Gemini returned an empty response.";
            }

            return result.trim();

        } catch (Exception e) {

            String reason = e.getMessage();

            if (reason == null || reason.isBlank()) {
                reason = e.getClass().getSimpleName();
            }

            return fallbackHeader
                    + "\n\n"
                    + "Gemini Smart Review unavailable.\n"
                    + "Model: "
                    + model
                    + "\n"
                    + "Reason: "
                    + reason;
        }
    }
}