from litellm import embedding
import pandas as pd

def get_embedding(text):
    """Vytvoří vektor z textu pomocí LiteLLM"""
    response = embedding(
        model="text-embedding-ada-002", # Volá Ollamu nebo Azure podle configu
        input=[text]
    )
    return response['data'][0]['embedding']

def json_to_markdown(json_data):
    """Převede JSON list na Markdown tabulku (pro lepší embedding)"""
    # Jednoduchá implementace (v reálu použij pandas.to_markdown())
    df = pd.DataFrame(json_data)
    return df.to_markdown(index=False)



