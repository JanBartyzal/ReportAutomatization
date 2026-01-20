import json
import redis.asyncio as redis
from functools import wraps
import os

class CacheManager:
    def __init__(self):
        redis_url = os.environ.get("REDIS_HOST",None)
        redis_port = os.environ.get("REDIS_PORT",6379)
        redis_url = f"redis://{redis_url}:{redis_port}"

        if (redis_url is None):
            redis_url = os.getenv("REDIS_URL",None)
        self.redis = redis.from_url(redis_url, decode_responses=True)

    async def get(self, key: str):
        data = await self.redis.get(key)
        return json.loads(data) if data else None

    async def set(self, key: str, value: dict, ttl: int = 3600):
        await self.redis.setex(key, ttl, json.dumps(value))

    async def delete(self, key: str):
        await self.redis.delete(key)
    
    async def delete_all_keys(self):
        keys = await self.redis.keys("*")
        if keys:
            await self.redis.delete(*keys)

# Instance, kterou budeš importovat
cache = CacheManager()