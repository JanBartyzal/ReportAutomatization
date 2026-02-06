import logging
import base64
import json
from typing import Dict, List, Optional, Any
import httpx
from app.core.config import settings

logger = logging.getLogger(__name__)


class OllamaOCRService:
  
    def __init__(self):
        """Initialize Ollama OCR service."""
        self.base_url = settings.ollama_base_url
        #self.model = settings.ollama_ocr_model
        self.model = "llava"
        #self.model = "glm-ocr:latest"
        self.timeout = settings.ollama_timeout
        
    async def extract_table_from_slide(
        self, 
        text: str, 
        image_bytes: bytes
    ) -> Dict[str, Any]:
        """
        Extract structured table data from a slide image and text.
        
        Args:
            text: Extracted text content from the slide
            image_bytes: PNG image of the slide
            
        Returns:
            Dictionary with structure:
            {
                "has_table": bool,
                "tables": [
                    {
                        "headers": [...],
                        "rows": [[...], ...]
                    }
                ],
                "text_content": str,
                "prompt": str
            }
        """
        try:
            # Encode image to base64
            image_base64 = base64.b64encode(image_bytes).decode('utf-8')
            
            # Build prompt for Ollama
            prompt = self._build_prompt(text)
            #print("Ollama Prompt: ", prompt)

            # Call Ollama API
            response_data = await self._call_ollama_api(prompt, image_base64)
            
            # Parse and validate response
            result = self._parse_response(response_data)
            result["prompt"] = prompt

            #print(result)
            
            logger.info(f"Ollama OCR extracted {len(result.get('tables', []))} table(s)")
            return result
            
        except Exception as e:
            logger.error(f"Ollama OCR extraction failed: {e}", exc_info=True)
            return {
                "has_table": False,
                "tables": [],
                "text_content": text,
                "error": str(e)
            }
    
    def _build_prompt(self, text: str) -> str:
        """
        Build the prompt for Ollama OCR model.
        
        Args:
            text: Extracted text from slide
            
        Returns:
            Formatted prompt string
        """
        prompt = f"""You are analyzing a PowerPoint slide image. The extracted text from this slide is:

        {text}

        Your task is to:
        1. Identify if there are any tables in the image
        2. If tables exist, extract them with proper structure (headers and data rows)
        3. Preserve all numeric values exactly as shown
        4. If a cell contains multiple values (e.g., "-21.4 +2.2" or "-3.2 -5.53 -0.3"), keep them together in one cell
        5. Return ONLY valid JSON in this exact format:

        {{
        "has_table": true or false,
        "tables": [
            {{
            "headers": ["column1", "column2", ...],
            "rows": [
                ["cell1", "cell2", ...],
                ["cell1", "cell2", ...]
            ]
            }}
        ],
        "text_content": "any non-table text content"
        }}

        IMPORTANT:
        - Return ONLY the JSON, no additional text or explanation
        - Be precise with numbers and formatting
        - If cells are empty or contain only dots/dashes, include them as "." or "-"
        - Preserve the original text exactly as it appears
        """
        return prompt
    
    async def _call_ollama_api(self, prompt: str, image_base64: str) -> Dict[str, Any]:
        """
        Call Ollama API to generate response.
        
        Args:
            prompt: The prompt text
            image_base64: Base64-encoded image
            
        Returns:
            API response data
            
        Raises:
            httpx.HTTPError: If API call fails
        """
        #url = f"{self.base_url}/api/generate"
        url = f"{self.base_url}/api/chat"
    
        #headers = {
        #    "Content-Type": "application/json",
        #    "Authorization": f"Bearer {self.api_key}"
        #}
        
        print("Ollama Model: ", self.model)
        payload = {
            "model": self.model,
            "messages": [
                {
                    "role": "system",
                    "content": "You are a helpful assistant for analyzing a PowerPoint slide image."
                },
                {
                    "role": "user",
                    "content": prompt,
                    "images": [image_base64]
                }
            ],
            "stream": False,
            "format": "json"
        }
        
        #print("Ollama Payload: ", payload)
        #print("Ollama URL: ", url)

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(url, json=payload)
            response.raise_for_status()
            #print("Ollama Response: ", response.json())
            jsonResponse = response.json()
            #print("Ollama JSON Response: ", jsonResponse)
            message = jsonResponse.get("message", "")
            #print("Ollama Message: ", message)
            content = message.get("content", "")
            #print("Ollama Content: ", content)
            jsonContent = json.loads(content)
            #print("Ollama JSON Content: ", jsonContent)

            return jsonContent
    
    def _parse_response(self, response_json: Dict[str, Any]) -> Dict[str, Any]:
        """
        Parse and validate Ollama API response.
        
        Args:
            response_json: json response content from chat/message
            
        Returns:
            Validated structured data
        """
        # Extract the response text
        #response_text = response_data.get("response", "{}")
        
        try:
            # Parse JSON
            result = response_json
            
            # Validate structure
            if not isinstance(result, dict):
                raise ValueError("Response is not a dictionary")
            
            # Ensure required fields
            result.setdefault("has_table", False)
            result.setdefault("tables", [])
            result.setdefault("text_content", "")
            
            # Validate tables structure
            for table in result.get("tables", []):
                if not isinstance(table, dict):
                    continue
                if "headers" not in table:
                    table["headers"] = []
                if "rows" not in table:
                    table["rows"] = []
            
            return result
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse Ollama response as JSON: {e}")
            logger.debug(f"Raw response: {response_json[:500]}")
            return {
                "has_table": False,
                "tables": [],
                "text_content": "",
                "error": f"Invalid JSON response: {str(e)}"
            }
    
    def convert_ocr_table_to_dict_list(self, table: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        Convert OCR table format to list of dictionaries.
        
        Converts from:
            {"headers": ["h1", "h2"], "rows": [["v1", "v2"]]}
        To:
            [{"h1": "v1", "h2": "v2"}]
        
        Args:
            table: Table dictionary with headers and rows
            
        Returns:
            List of row dictionaries
        """
        headers = table.get("headers", [])
        rows = table.get("rows", [])
        
        result = []
        for row in rows:
            row_dict = {}
            for i, header in enumerate(headers):
                value = row[i] if i < len(row) else None
                row_dict[header] = value
            result.append(row_dict)
        
        return result
