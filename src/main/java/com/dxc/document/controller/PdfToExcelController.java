package com.dxc.document.controller;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dxc.document.utility.ExcelWriterUtil;
import com.dxc.document.utility.PdfExtractor;

@RestController
public class PdfToExcelController {

	
	private static final Logger logger = LoggerFactory.getLogger(PdfToExcelController.class);
    private static final String EXCEL_PATH = "sheet.xlsx";

    @PostMapping(value = "/uploadfile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAndAppendExcel(@RequestParam("files") MultipartFile[] files) {
        if (files.length == 0) {
            logger.warn("Upload attempt with no files.");
            return ResponseEntity.badRequest().body("No files uploaded.");
        }

        try {
            Map<String, String> headers = null;
            List<Map<String, String>> extractedRows = new ArrayList<>();

            for (MultipartFile file : files) {
                File convFile = convertToFile(file);
                logger.info("Extracting data from: {}", file.getOriginalFilename());

                Map<String, String> extractedData = PdfExtractor.extractFieldsFromPdf(convFile);
                extractedRows.add(extractedData);

                if (headers == null) {
                    headers = new LinkedHashMap<>(extractedData);
                }
            }

            ExcelWriterUtil.appendDataToExcel(EXCEL_PATH, headers.keySet(), extractedRows);
            logger.info("Data appended to Excel successfully. Path: {}", EXCEL_PATH);

            return ResponseEntity.ok("Batch processed and data saved.");
        } catch (Exception e) {
            logger.error("Error during file processing: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process files.");
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFinalExcel() {
        File file = new File(EXCEL_PATH);

        if (!file.exists()) {
            logger.warn("Download requested but Excel file not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        try {
            logger.info("Preparing to download file: {}", EXCEL_PATH);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=final_sheet.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(resource);
        } catch (IOException e) {
            logger.error("Failed to read Excel file for download: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    private File convertToFile(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir"), file.getOriginalFilename());
        try (OutputStream os = new FileOutputStream(convFile)) {
            os.write(file.getBytes());
        }
        logger.debug("Converted MultipartFile to File: {}", convFile.getAbsolutePath());
        return convFile;
    }
}
