"""Prompt template management loaded from YAML configuration.

Templates are stored in a YAML file (not hardcoded) as required
by the task specification. Each template defines a system prompt
and a user prompt template with placeholder variables.
"""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Any

import yaml

from src.config import PROMPTS_CONFIG_PATH

logger = logging.getLogger(__name__)


class PromptService:
    """Loads and renders prompt templates from YAML config."""

    def __init__(self, config_path: str | None = None) -> None:
        self._config_path = Path(config_path or PROMPTS_CONFIG_PATH)
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
        """Build the messages list for a chat completion call.

        Args:
            operation_type: Template key (e.g., "CLASSIFY", "SUMMARIZE").
            **kwargs: Variables to substitute into the user prompt template.

        Returns:
            List of message dicts ready for the OpenAI-compatible API.

        Raises:
            KeyError: If the operation type has no template defined.
        """
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
