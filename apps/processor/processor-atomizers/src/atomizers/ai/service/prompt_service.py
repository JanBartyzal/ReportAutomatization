"""Prompt template management loaded from YAML configuration."""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Any

import yaml

logger = logging.getLogger(__name__)


class PromptService:
    """Loads and renders prompt templates from YAML config."""

    def __init__(self, config_path: str | None = None) -> None:
        if config_path is None:
            from src.common.config import Settings
            config_path = Settings().prompts_config_path
        self._config_path = Path(config_path)
        self._templates: dict[str, dict[str, Any]] = {}
        self._load()

    def _load(self) -> None:
        """Load prompt templates from YAML file."""
        if not self._config_path.exists():
            logger.warning("Prompts config not found at %s, using empty templates", self._config_path)
            return
        with self._config_path.open("r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
        self._templates = data.get("prompts", {})
        logger.info("Loaded %d prompt templates from %s", len(self._templates), self._config_path)

    def get_messages(self, operation_type: str, **kwargs: str) -> list[dict[str, str]]:
        """Build the messages list for a chat completion call."""
        template = self._templates.get(operation_type)
        if template is None:
            raise KeyError(f"No prompt template for operation_type={operation_type!r}")

        system_prompt = template["system"]
        user_template = template["user"]
        user_prompt = user_template.format(**kwargs)

        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]
