# Vector & RAG API

Endpoints for document vectorization and AI-powered semantic search using pgvector.

## Vectorize JSON

`POST /api/vector/vectorize_json`

Converts JSON table data to RAG-optimized markdown format, generates vector embeddings using OpenAI/LiteLLM, and indexes them in PostgreSQL.

### Request Body
- `json_data` (object): Slide data dictionary.
- `report_id` (int): ID of the parent report.
- `slide_index` (int): Index of the slide.

### Responses
- **200 OK**: Status message confirming indexing.

### Authentication
Required.

---

## Chat with Data

`POST /api/vector/chat-with-data`

Performs RAG-powered chat. Uses semantic search to find the most relevant data chunks and generates a natural language response via LLM.

### Request Body
- `query` (string): The user's question.

### Responses
- **200 OK**:
  - `answer` (string): LLM-generated response based on available data.
  - `sources` (list): Metadata of source chunks used for the answer.

### Workflow
1. Create embedding from user query.
2. Semantic search in PostgreSQL (pgvector cosine distance).
3. Retrieve top 5 most similar document chunks.
4. Build context prompt for LLM.
5. Generate answer using "Financial Analyst" system persona.

### Authentication
Required.
