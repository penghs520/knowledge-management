package com.enterprise.km.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentParserService {

    private final Tika tika = new Tika();

    public String parseDocument(MultipartFile file) {
        try {
            return parseDocumentContent(file.getInputStream(), file.getOriginalFilename());
        } catch (Exception e) {
            log.error("Error parsing document: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to parse document", e);
        }
    }

    private String parseDocumentContent(InputStream inputStream, String filename) throws Exception {
        String fileType = getFileType(filename);

        switch (fileType.toLowerCase()) {
            case "pdf":
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
                return parseTikaDocument(inputStream);
            case "md":
            case "txt":
                return parseTextDocument(inputStream);
            default:
                log.warn("Unsupported file type: {}", fileType);
                return parseTikaDocument(inputStream); // Try with Tika anyway
        }
    }

    private String parseTikaDocument(InputStream inputStream) throws Exception {
        BodyContentHandler handler = new BodyContentHandler(-1); // No limit
        Metadata metadata = new Metadata();
        AutoDetectParser parser = new AutoDetectParser();

        parser.parse(inputStream, handler, metadata);
        return handler.toString();
    }

    private String parseTextDocument(InputStream inputStream) throws Exception {
        return new String(inputStream.readAllBytes());
    }

    private String getFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public String detectMimeType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream());
        } catch (Exception e) {
            log.error("Error detecting MIME type", e);
            return "application/octet-stream";
        }
    }
}
