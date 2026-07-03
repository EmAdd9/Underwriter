package com.banking.underwriter.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class IngestionService {

    private final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void processAndStore(String applicationId, MultipartFile file) throws IOException {
        log.info("Starting ingestion processing for Application ID: {} | Filename: {}", applicationId, file.getOriginalFilename());

        var resource = new InputStreamResource(file.getInputStream());
        DocumentReader reader = "application/pdf".equals(file.getContentType())
                ? new PagePdfDocumentReader(resource)
                : new TikaDocumentReader(resource);

        log.debug("Using reader implementation: {}", reader.getClass().getSimpleName());
        List<Document> rawDocuments = reader.read();
        log.info("Extracted {} initial text segment blocks from source document.", rawDocuments.size());

        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .build();

        List<Document> splitDocuments = textSplitter.apply(rawDocuments);
        log.debug("Document fragmented into {} tokens/chunks post splitting parameters.", splitDocuments.size());

        for (Document doc : splitDocuments) {
            doc.getMetadata().put("applicationId", applicationId);
        }

        log.info("Writing isolated document chunks into vector database store for Application ID: {}", applicationId);
        vectorStore.write(splitDocuments);
        log.info("Document sync ingestion pipeline complete for Application ID: {}", applicationId);
    }
}