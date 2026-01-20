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