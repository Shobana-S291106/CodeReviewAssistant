package com.codereviewassistant.backend.service;

import com.codereviewassistant.backend.analyzer.JavaCodeAnalyzer;
import com.codereviewassistant.backend.dto.AnalysisResult;
import com.codereviewassistant.backend.dto.FileUploadResponse;
import com.codereviewassistant.backend.dto.ReviewIssue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@Service
public class FileService {

    private final String uploadDir =
            System.getProperty("user.dir")
                    + File.separator
                    + "uploads";

    @Autowired
    private JavaCodeAnalyzer javaCodeAnalyzer;

    @Autowired
    private GeminiService geminiService;

    public FileUploadResponse uploadFile(MultipartFile file) throws Exception {

        // =====================================================
        // 1. CREATE UPLOAD DIRECTORY
        // =====================================================

        File dir = new File(uploadDir);

        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create uploads directory."
            );
        }

        // =====================================================
        // 2. VALIDATE UPLOADED FILE
        // =====================================================

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uploaded file is empty."
            );
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid file name."
            );
        }

        // =====================================================
        // 3. VALIDATE JAVA FILE
        // =====================================================

        if (!originalFileName.toLowerCase().endsWith(".java")) {
            throw new IllegalArgumentException(
                    "Only Java (.java) files are supported."
            );
        }

        // =====================================================
        // 4. SAVE UPLOADED FILE
        // =====================================================

        File destination = new File(dir, originalFileName);

        file.transferTo(destination);

        // =====================================================
        // 5. READ SOURCE CODE
        // =====================================================

        String sourceCode =
                Files.readString(destination.toPath());

        // =====================================================
        // 6. STATIC CODE ANALYSIS
        // =====================================================

        AnalysisResult analysisResult =
                javaCodeAnalyzer.analyze(sourceCode);

        // =====================================================
        // 7. SMART REVIEW
        // =====================================================

        String aiReview =
                geminiService.reviewCode(sourceCode);

        // =====================================================
        // 8. BUILD FINAL REPORT
        // =====================================================

        String finalReport =
                "========== LOCAL STATIC ANALYSIS ==========\n\n"
                + analysisResult.getReport()
                + "\n\n"
                + "Score: "
                + analysisResult.getScore()
                + "/100"
                + "\n\n"
                + "========== SMART REVIEW ==========\n\n"
                + aiReview;
        // =====================================================
// 9. USE STATIC ANALYZER ISSUES
// =====================================================

        List<ReviewIssue> structuredIssues =
                analysisResult.getIssues();

// =====================================================
// 10. RETURN RESPONSE
// =====================================================

        return new FileUploadResponse(
                originalFileName,
                "File uploaded and analyzed successfully.",
                finalReport,
                structuredIssues,
                analysisResult.getScore()
        );

    }
}