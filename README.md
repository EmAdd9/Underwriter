## ⚙️ Step-by-Step Setup, Build & Execution Guide

Follow this comprehensive walkthrough to configure your environment variables, compile the microservice, and execute end-to-end API testing against the automated underwriting engine.

---

### Step 1: Set Up Your OpenAI Environment Variable
The application retrieves your API secret safely from your operating system environment variables to authenticate with the Spring AI ecosystem. Execute the corresponding command for your specific terminal shell:

*   **Linux / macOS (Bash/Zsh):**
    ```bash
    export OPENAI_API_KEY="your-actual-api-key-here"
    ```
*   **Windows (Command Prompt / CMD):**
    ```cmd
    set OPENAI_API_KEY=your-actual-api-key-here
    ```
*   **Windows (PowerShell):**
    ```powershell
    $env:OPENAI_API_KEY="your-actual-api-key-here"
    ```

---

### Step 2: Build & Run the Application
Navigate to your project root directory containing the `pom.xml` file and run standard Maven directives to compile the Java layers and spin up the embedded web application container:

```bash
# Force resolution of submodules, clean target space, and compile
mvn clean compile

# Launch the embedded Tomcat application server
mvn spring-boot:run

# 🧪 Automated Underwriting System: QA & Testing Notes

This guide provides testing notes, verification steps, and exact cURL commands with their corresponding responses to validate the end-to-end execution of the RAG-driven underwriting compliance pipeline.

---

## 📋 Pre-Testing Notes & Requirements

*   **Stateful Sequencing:** You must execute **Phase 1** (Ingestion) before running **Phase 2** (Decision Engine). If the Vector Store doesn't contain chunks matching your target `applicationId`, the RAG layer will fail to extract document context, resulting in automatic policy evaluation holds.
*   **In-Memory Lifecycle:** The application initializes with an embedded H2 database. Restarting the Spring Boot application clears all database records and active policy profiles, reverting the environment back to the "Baseline Default" framework ruleset.
*   **API Client Support:** The cURL commands below can be executed directly in your terminal or imported seamlessly into Postman or Insomnia by pasting the raw text into the request URL box.

---

## 🔍 Interactive API Verification Steps

### Phase 1: Ingest & Vectorize Customer Financial Files
This endpoint processes unstructured multi-page documents, generates text chunk vectors via OpenAI's embedding model, and binds an isolation metadata attribute (`applicationId`) to guarantee data privacy.

*   **HTTP Method:** `POST`
*   **URL:** `http://localhost:8080/api/v1/underwrite/APP-78922/upload`
*   **Content-Type:** `multipart/form-data`

#### cURL Request
```bash
curl -X POST http://localhost:8080/api/v1/underwrite/APP-78922/upload \
  -H "Accept: text/plain" \
  -F "file=@/path/to/applicant_bank_statement.pdf"

```bash
curl -X GET http://localhost:8080/api/v1/underwrite/APP-78922/decision \
  -H "Accept: application/json"

```json
{
  "state": "APPROVED",
  "causes": [
    "APPROVAL_CAUSE: Auto-verification evaluation cleared fully against standard rule system: Baseline Default"
  ]
}

```json
{
  "state": "REJECTED",
  "causes": [
    "REJECTION_CAUSE: Income 22000.0 is lower than the required threshold of 30000.0",
    "REJECTION_CAUSE: Calculated DTI ratio of 52.50% exceeds dynamic ceiling of 45.0%"
  ]
}

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

