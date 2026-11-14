package com.axedu.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.axedu.common.response.ApiResponse;
import com.axedu.common.security.AuthUser;
import com.axedu.common.security.CurrentUser;
import com.axedu.config.WebConfig;
import com.axedu.dto.file.UploadedFileResponse;
import com.axedu.service.FileService;

import lombok.RequiredArgsConstructor;

/** 파일 업로드 (계약서 3.9) + 서명 URL 로 파일 열기 */
@RestController
@RequestMapping(WebConfig.API_BASE + "/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /** multipart/form-data, 필드명 file */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadedFileResponse>> upload(@CurrentUser AuthUser user, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(fileService.upload(user, file)));
    }

    /** 서명 URL (인증 헤더 없음). 만료·위조면 404 */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<Resource> content(@PathVariable String fileId, @RequestParam long expires, @RequestParam String sig) {
        FileService.FileContent fc = fileService.open(fileId, expires, sig);
        ContentDisposition disposition = (fc.file().isImage() ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(fc.file().getOriginalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fc.file().getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(fc.resource());
    }
}
