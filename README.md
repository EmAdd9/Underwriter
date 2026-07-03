# Underwriter
Automated Underwriting System Blueprint
Framework Stack: Spring Boot 3.x, Spring AI, Vector DB (PGVector/Redis), Hibernate JPA.
This technical architecture blueprint maps out an isolated, multi-tenant financial document analysis and automated
compliance pipeline via Retrieval-Augmented Generation (RAG).
Architecture Overview
• 
• 
Isolated Tenant Ingestion: All text chunks are tagged with a unique applicationId during the reader
stage to guarantee data privacy.
Deterministic Engine: The LLM is restricted to deterministic numerical extraction via JSON Schema
constraints. State tracking and policy criteria boundaries are evaluated safely in Java code
