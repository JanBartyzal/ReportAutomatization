"""
RAG (Retrieval Augmented Generation) utilities.

This module provides utilities for creating text embeddings and converting
JSON data to markdown format for better semantic search results.
"""

from typing import List, Dict, Any
import pandas as pd
from litellm import embedding


def get_embedding(text: str) -> List[float]:
    """
    Create vector embedding from text using LiteLLM.
    
    Routes to appropriate embedding provider (Ollama or Azure OpenAI)
    based on LiteLLM configuration.
    
    Args:
        text: Text to embed
        
    Returns:
        Vector embedding as list of floats
        
    Example:
        >>> vector = get_embedding("Sample table data")
        >>> len(vector)
        1536  # For text-embedding-ada-002
    """
    response = embedding(
        model="text-embedding-ada-002",
        input=[text]
    )
    return response['data'][0]['embedding']


def json_to_markdown(json_data: List[Dict[str, Any]]) -> str:
    """
    Convert JSON list of dictionaries to Markdown table.
    
    This format improves embedding quality for table data by providing
    better structural representation for semantic search.
    
    Args:
        json_data: List of dictionaries representing table rows
        
    Returns:
        Markdown-formatted table as string
        
    Example:
        >>> data = [{"Name": "John", "Age": 30}, {"Name": "Jane", "Age": 25}]
        >>> print(json_to_markdown(data))
        | Name   |   Age |
        |:-------|------:|
        | John   |    30 |
        | Jane   |    25 |
    """
    df = pd.DataFrame(json_data)
    return df.to_markdown(index=False)



