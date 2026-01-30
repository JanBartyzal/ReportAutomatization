"""
Redis cache manager using async client.

This module provides a singleton cache manager for Redis operations.
All methods are async to work properly with FastAPI's async route handlers.
"""

import json
import redis.asyncio as redis
from typing import Any, Optional
from app.core.config import settings


class CacheManager:
    """
    Async Redis cache manager.
    
    Provides methods for getting, setting, and deleting cached data.
    All data is automatically serialized to/from JSON.
    """
    
    def __init__(self) -> None:
        """Initialize Redis client from centralized configuration."""
        self.redis = redis.from_url(
            str(settings.redis_url),
            decode_responses=True
        )

    async def get(self, key: str) -> Optional[Any]:
        """
        Get a value from cache.
        
        Args:
            key: Cache key
            
        Returns:
            Deserialized value if key exists, None otherwise
        """
        data = await self.redis.get(key)
        return json.loads(data) if data else None

    async def set(self, key: str, value: Any, ttl: Optional[int] = None) -> None:
        """
        Set a value in cache with optional TTL.
        
        Args:
            key: Cache key
            value: Value to cache (will be JSON serialized)
            ttl: Time-to-live in seconds (uses settings default if None)
        """
        ttl = ttl or settings.cache_ttl_seconds
        await self.redis.setex(key, ttl, json.dumps(value))

    async def delete(self, key: str) -> None:
        """
        Delete a key from cache.
        
        Args:
            key: Cache key to delete
        """
        await self.redis.delete(key)
    
    async def delete_all_keys(self) -> None:
        """
        Delete all keys from cache.
        
        WARNING: This will flush the entire Redis database.
        Use with caution, especially in production.
        """
        keys = await self.redis.keys("*")
        if keys:
            await self.redis.delete(*keys)


# Singleton cache instance
cache = CacheManager()