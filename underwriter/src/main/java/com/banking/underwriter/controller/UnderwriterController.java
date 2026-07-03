package com.banking.underwriter.controller;


import com.banking.underwriter.model.UnderwritingDecision;
import com.banking.underwriter.service.IngestionService;
import com.banking.underwriter.service.UnderwritingEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/underwrite")
public class UnderwriterController {

    private final IngestionService ingestionService;
    private final UnderwritingEngineService engineService;

    public UnderwriterController(IngestionService ingestionService, UnderwritingEngineService engineService) {
        this.ingestionService = ingestionService;
        this.engineService = engineService;
    }

    @PostMapping("/{applicationId}/upload")
    public ResponseEntity<String> uploadDocuments(
            @PathVariable String applicationId,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("HTTP POST -> Request received to ingest file document for Application ID: {}", applicationId);
        ingestionService.processAndStore(applicationId, file);
        return ResponseEntity.ok("File ingested and indexed successfully under Application: " + applicationId);
    }

    @GetMapping("/{applicationId}/decision")
    public ResponseEntity<UnderwritingDecision> fetchApprovalDecision(@PathVariable String applicationId) {
        log.info("HTTP GET -> Routing decision matrix calculation verification request for Application ID: {}", applicationId);
        UnderwritingDecision decision = engineService.evaluateApplication(applicationId);
        return ResponseEntity.ok(decision);
    }
}
