<div align="center">

# 🛡️ Automated Underwriting Compliance Engine

An intelligent, RAG-driven underwriting and compliance system built with **Spring Boot**, **Spring AI**, **Google GenAI / Gemini**, and **PostgreSQL (`pgvector`)**. 

*Ingests unstructured financial documents, vectorizes them with isolated metadata scoping (`applicationId`), and evaluates them dynamically against strict credit risk policies.*

---

[<img src="https://img.shields.io/badge/Spring%2520Boot-3.x-brightgreen.svg" alt="Spring Boot">](https://spring.io/projects/spring-boot)
[<img src="https://img.shields.io/badge/Spring%2520AI-Integration-blue.svg" alt="Spring AI">](https://spring.io/projects/spring-ai)
[<img src="https://img.shields.io/badge/Google%2520Gemini-GenAI-orange.svg" alt="Google GenAI">](https://cloud.google.com/gemini)
[<img src="https://img.shields.io/badge/PostgreSQL-pgvector-purple.svg" alt="PostgreSQL pgvector">](https://github.com/pgvector/pgvector)
[<img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">](LICENSE)

</div>

---

## 🚀 Architectural Overview

```
[ Unstructured PDF/TXT Bank Statement ]
                   │
                   ▼ (Multipart File Stream)
         [ Ingestion Service ] 
                   │
                   ▼ (Token Text Splitter & Tika Reader)
      [ Google GenAI Embedding Engine ]
                   │
                   ▼ (Vector Embedding - 3072 Dimensions)
      [ PostgreSQL + pgvector (metadata: applicationId) ]
                   │
                   ▼ (Retrieval-Augmented Generation / RAG)
    [ Underwriting Policy Decision Engine & LLM Analysis ]
                   │
     ┌─────────────┼─────────────┐
     ▼             ▼             ▼
 `APPROVED`    `REJECTED`     `PENDING` (Human Review)
```

---

## ⚙️ Step-by-Step Setup, Build & Execution Guide

Follow this walkthrough to configure your environment variables, spin up your database, compile the microservice, and execute end-to-end API testing.

### Step 1: Set Up Your API Environment Variable
The application retrieves your API secret safely from your operating system environment variables to authenticate with the AI ecosystem. Execute the corresponding command for your terminal shell:

* **Linux / macOS (Bash / Zsh):**
  ```bash
  export SPRING_AI_GOOGLE_GENAI_API_KEY="your-actual-api-key-here"
  ```
* **Windows (Command Prompt / CMD):**
  ```cmd
  set SPRING_AI_GOOGLE_GENAI_API_KEY=your-actual-api-key-here
  ```
* **Windows (PowerShell):**
  ```powershell
  $env:SPRING_AI_GOOGLE_GENAI_API_KEY="your-actual-api-key-here"
  ```

### Step 2: Spin Up Infrastructure & Build the Application
1. Start your PostgreSQL vector database using Docker Compose:
   ```bash
   docker-compose up -d
   ```
2. Navigate to your project root directory containing the `pom.xml` file and compile:
   ```bash
   mvn clean compile
   ```
3. Launch the embedded Tomcat application server:
   ```bash
   mvn spring-boot:run
   ```

---

## 🧪 Testing & Experimentation Guide

### 📋 Pre-Testing Notes & Requirements
* **Stateful Sequencing:** You must execute **Phase 1 (Ingestion)** before running **Phase 2 (Decision Engine)**. If the Vector Store doesn't contain chunks matching your target `applicationId`, the RAG layer will fail to extract context, resulting in automatic policy holds.
* **Database Backend:** Ensure your PostgreSQL instance with the `pgvector` extension is active on port `5433`.

---

## 🔍 Interactive API Verification Steps

### Phase 1: Ingest & Vectorize Customer Financial Files
This endpoint processes unstructured documents, generates text chunk vectors, and binds an isolation metadata attribute (`applicationId`) to guarantee data privacy.

* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/v1/underwrite/APP-78922/upload`
* **Content-Type:** `multipart/form-data`

```bash
curl -X POST http://localhost:8080/api/v1/underwrite/APP-78922/upload \
  -H "Accept: text/plain" \
  -F "file=@/path/to/applicant_bank_statement.pdf"
```

---

### Phase 2: Trigger Automated Underwriting Evaluation
Once ingested, fetch the automated decision assessment for the specific application reference.

* **HTTP Method:** `GET`
* **URL:** `http://localhost:8080/api/v1/underwrite/APP-78922/decision`

```bash
curl -X GET http://localhost:8080/api/v1/underwrite/APP-78922/decision \
  -H "Accept: application/json"
```

---

## 📊 Expected Evaluation Responses & Outcomes

Depending on the financial profile and document integrity of the uploaded statement, the engine will return one of the following states:

### 1. Approved State (`APPROVED`)
Returned when net monthly income clears thresholds, DTI is within acceptable limits, and no formatting anomalies exist.
```json
{
  "state": "APPROVED",
  "causes": [
    "APPROVAL_CAUSE: Auto-verification evaluation cleared fully against standard rule system: Baseline Default"
  ]
}
```

### 2. Rejected State (`REJECTED`)
Returned when financial rules are breached (e.g., low income or excessive debt-to-income ratio).
```json
{
  "state": "REJECTED",
  "causes": [
    "REJECTION_CAUSE: Income 22000.0 is lower than the required threshold of 30000.0",
    "REJECTION_CAUSE: Calculated DTI ratio of 52.50% exceeds dynamic ceiling of 45.0%"
  ]
}
```

### 3. Pending / Human Review State (`PENDING`)
Returned when document anomalies, watermarks, text corruption, or summary-to-ledger mismatches are detected, routing the application for manual underwriter review.
```json
{
  "state": "PENDING",
  "causes": [
    "PENDING_CAUSE: Document formatting variations reported: Ending balance mismatch: summary ending balance of $69,450.00 does not match final transaction ledger balance of $68,889.60 due to total debits summary omitting POS debit and utilities."
  ]
}
```

---

## ⚙️ Administrative Policy Management
You can dynamically configure custom risk profiles and thresholds via the admin policy endpoint:

```bash
curl -X POST http://localhost:8080/api/v1/admin/policies \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "policyName": "High Net Worth Tier Rules",
    "minNetIncome": 75000.0,
    "maxDtiRatio": 35.0,
    "allowBouncedCheques": false,
    "active": true
  }'
```

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.
