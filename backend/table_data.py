import re
import hashlib
import redis
import json
import os
import pandas as pd
from redis_cache import cache   
from images_tesseract import TableImageData

class TableDataProcessor:
    def __init__(self):
        self.redis_client = redis.Redis.from_url(os.getenv("REDIS_URL", "redis://localhost:6379/0"))
        self.CACHE_TTL = 60 * 60 * 24 * 7  # 7 dní
        self.image_data = TableImageData()

    def sanitize_table_name(self, title):
        clean = re.sub(r'[^a-zA-Z0-9]', '_', title).lower()
        return f"report_{clean}"


    def save_to_specific_table(self, df, detected_title, engine):
        table_name = self.sanitize_table_name(detected_title)
        df.to_sql(table_name, engine, if_exists='append', index=False)
        
        return table_name

    def get_image_hash(self, image_bytes):
        return hashlib.sha256(image_bytes).hexdigest()
    
    def extract_data(self, image_bytes):
        img_hash = self.get_image_hash(image_bytes)
        cache_key = f"img_extract:{img_hash}"
    
        cached_result = cache.get(cache_key)
        if cached_result:
            print(f"⚡ CACHE HIT: {img_hash[:8]}... (načítám z Redisu)")
            return json.loads(cached_result)

        print(f"🐢 CACHE MISS: {img_hash[:8]}... (počítám)")
        result = self.image_data.smart_extract(image_bytes)
        cache.set(cache_key, json.dumps(result), ex=self.CACHE_TTL)
    
        return result

    def normalize_text(self, text_content: list) -> str:
        """
        Takes a list of strings (text lines) and returns a single string with sequential line numbering.
        """
        combined_text_lines = []
        line_counter = 1
        
        if isinstance(text_content, list):
            for line in text_content:
                if line and str(line).strip():
                    combined_text_lines.append(f"{line_counter}. {str(line).strip()}")
                    line_counter += 1
        elif isinstance(text_content, str) and text_content.strip():
             combined_text_lines.append(f"1. {text_content.strip()}")
             
        return "\n".join(combined_text_lines)

    def normalize_table(self, table_data: list) -> pd.DataFrame:
        """
        Takes a list of dictionaries (rows) and converts it to a pandas DataFrame.
        """
        if table_data and isinstance(table_data, list):
             try:
                 df = pd.DataFrame(table_data)
                 if not df.empty:
                     return df
             except Exception as e:
                 print(f"Error converting table data to DataFrame: {e}")
        return pd.DataFrame()