package com.codereviewassistant.backend.service;

import com.codereviewassistant.backend.analyzer.JavaCodeAnalyzer;
import com.codereviewassistant.backend.dto.AnalysisResult;
import com.codereviewassistant.backend.dto.FileUploadResponse;
import com.codereviewassistant.backend.dto.ReviewIssue;
import com.codereviewassistant.backend.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public FileUploadResponse uploadFile(
            MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uploaded file is empty."
            );
        }

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid file name."
            );
        }

        if (!originalFileName
                .toLowerCase()
                .endsWith(".java")) {
            throw new IllegalArgumentException(
                    "Only Java (.java) files are supported."
            );
        }

        String safeFileName =
                Paths.get(originalFileName)
                        .getFileName()
                        .toString();

        if (safeFileName.isBlank()
                || !safeFileName
                        .toLowerCase()
                        .endsWith(".java")) {
            throw new IllegalArgumentException(
                    "Invalid Java file name."
            );
        }

        Path uploadPath =
                Paths.get(uploadDir)
                        .toAbsolutePath()
                        .normalize();

        Files.createDirectories(uploadPath);

        Path destination =
                uploadPath
                        .resolve(safeFileName)
                        .normalize();

        if (!destination.startsWith(uploadPath)) {
            throw new IllegalArgumentException(
                    "Invalid file path."
            );
        }

        file.transferTo(destination.toFile());

        String sourceCode =
                Files.readString(
                        destination,
                        StandardCharsets.UTF_8
                );

        // LOCAL STATIC ANALYSIS
        AnalysisResult analysisResult =
                javaCodeAnalyzer.analyze(sourceCode);

        List<ReviewIssue> structuredIssues =
                analysisResult.getIssues();

        // SMART REVIEW
        String aiReview;

        try {
            aiReview =
                    geminiService.reviewCode(sourceCode);

        } catch (Exception e) {

            aiReview =
                    "Smart Review unavailable.\n"
                    + "Local static analysis was completed "
                    + "successfully.";
        }

        // FINAL REPORT
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

        return new FileUploadResponse(
                safeFileName,
                "File uploaded and analyzed successfully.",
                finalReport,
                structuredIssues,
                analysisResult.getScore()
        );
    }
}