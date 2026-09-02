package com.codereviewassistant.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codereviewassistant.backend.dto.FileUploadResponse;
import com.codereviewassistant.backend.service.FileService;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://codereviewassistant-0qq7.onrender.com"
})
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file) throws Exception {

        FileUploadResponse response =
                fileService.uploadFile(file);

        return ResponseEntity.ok(response);
    }
}