# Universal Project Standards

**Context:** This document defines the coding standards, architectural guidelines, and documentation requirements for this project.
**Audience:** All human developers and AI Agents contributing to the codebase.


## 1. General Principles
- **Language:** English is mandatory for all code, comments, commit messages, and documentation.
- **KISS \& DRY:** Keep It Simple, Stupid. Don't Repeat Yourself. Prefer readability over cleverness.
- **Functional Style:** Prefer pure functions and immutability where possible (especially in React).
- **Type Safety:** Strong typing is strictly required across all stacks to reduce AI hallucination.

## 2. Tech Stack \& Guidelines

### 🐍 Python (Backend/Scripts)
- **Style:** Follow \[PEP 8](https://peps.python.org/pep-0008/).
- **Typing:** 
  - **Mandatory** Type Hints (`typing` module) for all function arguments and return values.
- **Docstrings:** Use Google-style docstrings.
- **Error Handling:** Use custom exceptions; never catch bare `Exception`.

### ⚛️ React (Frontend)
- **Style:** Functional components with Hooks only (no Class components).
- **State Management:** Context API for simple state, specific libraries only if defined in `{{PROJECT\_SPECIFIC\_CONFIG}}`.
- **Styling:** Modular CSS or Styled Components (consistency required per module).
- **Props:** Explicit Prop interfaces (TypeScript) or PropTypes are required.

## 3. Documentation Standards
- **Module Level:** Each distinct module/folder MUST have a `README.md` explaining:
  - 1. Purpose of the module.
  - 2. Key dependencies.
  - 3. Example usage.
- **Code Level:**
  - Complex logic (> 5 lines of calculation) requires a comment explaining \*why\*, not \*what\*.
  - Public API endpoints must be documented (Swagger/OpenAPI format preferred).

## 4. Specific Project Overrides
- **Architecture Pattern:** {{ INSERT: e.g., Clean Architecture, MVC, Microservices }}
- **Testing Framework:** {{ INSERT: e.g., PyTest, Jest, xUnit }}
- **Specific Libraries:** {{ INSERT: List of allowed/disallowed libs }}

**Note to AI Agents:** If a user request conflicts with these standards, prioritize these standards unless explicitly instructed to "ignore standards".

