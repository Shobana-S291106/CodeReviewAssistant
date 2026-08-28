package com.codereviewassistant.backend.controller;

import com.codereviewassistant.backend.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private GeminiService geminiService;

    // =====================================================
    // EXPLAIN AN ISSUE
    // =====================================================

    @PostMapping("/explain")
    public ResponseEntity<?> explainIssue(
            @RequestBody Map<String, String> request) {

        try {

            if (request == null) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Request body is required."
                        )
                );
            }

            String issue = request.get("issue");
            String code = request.get("code");

            if (issue == null || issue.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Issue description is required."
                        )
                );
            }

            String explanation =
                    geminiService.explainIssue(issue, code);

            return ResponseEntity.ok(
                    Map.of(
                            "explanation",
                            explanation
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "message",
                            "Unable to generate issue explanation."
                    )
            );
        }
    }

    // =====================================================
    // SUGGEST FIX
    // =====================================================

    @PostMapping("/suggest-fix")
    public ResponseEntity<?> suggestFix(
            @RequestBody Map<String, String> request) {

        try {

            if (request == null) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Request body is required."
                        )
                );
            }

            String issue = request.get("issue");
            String code = request.get("code");

            if (issue == null || issue.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Issue description is required."
                        )
                );
            }

            String fix =
                    geminiService.suggestFix(issue, code);

            return ResponseEntity.ok(
                    Map.of(
                            "fix",
                            fix
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "message",
                            "Unable to generate suggested fix."
                    )
            );
        }
    }
}