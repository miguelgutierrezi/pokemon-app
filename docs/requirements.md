# Requirements — Java Technical Interview Exercise (Ballast Lane)

> **Source of truth**: [`requirements.pdf`](./requirements.pdf) ("Java - BLA -
> Technical Interview Exercise - V2"). This file is a literal transcription of
> that PDF, plus **stable IDs** (in bold brackets) added only for traceability.
> The IDs are the only addition; wording is unchanged. If this file and the PDF
> ever disagree, the PDF wins.
>
> User stories are referred to as **HU-01..HU-04** throughout the project
> ("Historia de Usuario"), mapping 1:1 to the PDF's "User Story 01..04".
> They MUST NOT be added to, renamed, or merged.

---

## Project Overview

The objective of this exercise is the creation of a RESTful API leveraging Java
and Spring Boot. You are expected to employ Clean Architecture and TDD
methodologies to build a robust solution backed by a reliable data store.

Implementation must be guided by the specific Pokemon user stories detailed
hereafter; these requirements should be clearly integrated into your final
interview presentation.

This service will interface with the PokeAPI to facilitate data retrieval,
local replication, and attribute modification for various Pokemon entries.

Refer to the official PokeAPI documentation for integration details:
<https://pokeapi.co/docs/v2>

## Requirements

### Functional Requirements

- **[FR-POKEAPI]** PokeAPI Integration
  - Construct a RESTful API using Spring Boot that communicates with the
    external PokeAPI.
- **[HU-01]** User Story 01: Pokemon Enumeration
  - The system should allow users to browse Pokemon via paginated results,
    displaying each entry's sprite, category, mass, and a collection of their
    skills.
    - Nice to have: Implement caching for service responses.
- **[HU-02]** User Story 02: Detailed View
  - Users must be able to access comprehensive data for a chosen Pokemon,
    specifically viewing its image, core statistics, narrative description,
    and evolutionary lineage.
- **[HU-03]** User Story 03: Data Synchronization
  - Develop a mechanism to persist Pokemon data into a local relational store.
  - This replication layer is intended to facilitate the addition of
    proprietary fields.
    - Use cases include localized nomenclature, geographical metadata, or
      internal classification tags.
- **[HU-04]** User Story 04: Local Data Modification
  - Enable update operations for any Pokemon currently stored within the local
    database.
  - Ensure robust validation: provide 404 responses for missing records, 400
    status codes for malformed payloads, and incorporate further defensive
    logic as required.

### Technical Requirements

- Mandatory
  - **[TR-REPO]** Host the code in a public Git repository
  - **[TR-TESTS]** Include tests
  - **[TR-ERRORS]** Proper error handling
  - **[TR-CACHE]** Nice to have: Implement a caching layer for PokeAPI
    responses
  - **[TR-FRONTEND]** Front-end that consumes the API (language of your
    choice)
- Optional
  - **[TR-EXTRA]** Any additional functionality is welcome
- Database
  - **[TR-DB]** Establish a relational database or appropriate data storage
    solution containing a primary entity and a secondary collection for user
    management.
    - Records must include a unique primary key and a minimum of two
      descriptive attributes.
- API
  - **[TR-API-CRUD]** Construct an Java Web API facilitating comprehensive
    CRUD operations for the defined dataset.
    - Ensure all endpoints utilize standard HTTP verbs, required parameters,
      and consistent return structures.
  - **[TR-API-AUTH]** Implement an auxiliary API for user registration,
    authentication, and the management of protected versus public routes.
- Data Layer
  - **[TR-DATA]** Design a specialized data access layer to manage
    interactions with the persistence store, providing the foundational logic
    for the API controllers.
- Core Business Logic
  - **[TR-CORE]** Develop a dedicated business logic layer to encapsulate all
    domain rules and data validation procedures.
  - This layer should maintain architectural independence from both the API
    and the data access components.
- Testing and Validation
  - **[TR-UNIT]** Provide thorough unit test coverage for every core component
    within the application suite.

### Frontend

- While the primary emphasis resides in backend proficiency, a comprehensive
  demonstration of full-stack development capabilities is required.
  Consequently:
  - **[FE-INTEGRATION]** Integrate your established backend service with a
    modern frontend framework of your choosing (e.g., React or Vue).
  - Primary Evaluation Criteria:
    - **[FE-RESPONSIVE]** The interface must exhibit responsiveness and a
      user-centric design.
    - **[FE-CRUD]** Execute standard CRUD operations corresponding to the
      defined functional use cases.
    - **[FE-ARCH]** Architectural integrity: Maintain clean component
      organization and efficient state management.
- Submission Guidelines:
  - **[SUB-README]** Provide a comprehensive README detailing environment
    configuration and pertinent technical documentation.
  - **[SUB-SEED]** The application should be pre-populated with seeded data or
    mock credentials for demonstration purposes.

## Delivery

- **[DEL-README]** Provide a README file containing environment setup
  procedures and technical documentation.
- **[DEL-SEED]** The solution must be pre-populated with relevant
  demonstration data.
- **[DEL-DOCKER]** Supply a Dockerfile for containerized execution.

## Generative AI tools

**[GENAI]** Imagine you're tasked with generating a RESTful API for a simple
task management system using your preferred language. The system should
support the following functionality:

- Create, read, update, and delete tasks (CRUD)
- Each task has a title, description, status, and due_date
- Tasks are associated with a user (assume basic User model exists)

Instructions:

- Using your preferred GenAI coding tool (e.g., Cursor, Claude Code, Windsurf,
  GitHub Copilot, etc.), write the prompt you would use to generate the API
  scaffold or full implementation.
- Show the output code (or a representative sample of it).
- Describe how you:
  - Validated the AI's suggestions
  - Corrected or improved the output, if necessary
  - Handled edge cases, authentication, or validations

## Presentation and Code Review

You will be required to present your project to the technical interview panel.
During the presentation, you should explain your user story, design choices,
the technical architecture, and demonstrate the functionality of the
application. This will be done over Google Meets or Zoom and you will screen
share either your GitHub repository or IDE.

After the presentation, the interview panel will conduct a code review of your
project. You will be asked to explain your coding decisions and answer any
questions related to the code. The interview panel will evaluate your project
based on the following criteria:

- **[EVAL-ARCH]** Clean Architecture: Your architecture should adhere to Clean
  Architecture principles, including separation of concerns and independence
  of components.
- **[EVAL-TEST]** Application testing: Your project should have sufficient
  test coverage. Use of TDD is preferable.
- **[EVAL-QUALITY]** Code quality: Your code should be well-organized,
  readable, and adhere to best practices.
- **[EVAL-FUNC]** Functionality: Your application should perform as expected
  in the requirements without errors or bugs. Optional but desired: no
  warnings in the browser console.
- **[EVAL-PRES]** Presentation: Your presentation should be clear, concise,
  and demonstrate a good understanding of the project and of the main backend
  and frontend best practices.
- **[EVAL-GENAI]** GenAI tools: Your answers and presentation must show
  fluency with GenAI tools and prompt engineering, and critical thinking when
  evaluating AI-generated code.

---

## Traceability map (project-level, not part of the PDF)

Which feature branch delivers each requirement. Every spec MUST cite the IDs it
covers.

| Feature branch | Covers |
|---|---|
| `001-setup` | TR-REPO, DEL-DOCKER, DEL-README / SUB-README (skeleton), EVAL-ARCH (enforcement), TR-TESTS (tooling) |
| `002-hu01-pokemon-enumeration` | HU-01, FR-POKEAPI, TR-CACHE, TR-ERRORS |
| `003-hu02-detailed-view` | HU-02 |
| `004-hu03-data-sync` | HU-03, TR-DB (primary entity), TR-DATA, DEL-SEED / SUB-SEED (Pokémon data) |
| `005-hu04-local-update` | HU-04, TR-CORE (validation), TR-API-CRUD (Delete of a local record) |
| `006-auth` | TR-API-AUTH, TR-DB (user collection), SUB-SEED (mock credentials) |
| `007-frontend` | TR-FRONTEND, FE-INTEGRATION, FE-RESPONSIVE, FE-CRUD, FE-ARCH |
| `docs/genai/` (no feature branch; delivered alongside 007 or as its own docs PR) | GENAI, EVAL-GENAI |

Interpretation, recorded on purpose: **TR-API-CRUD** asks for "comprehensive
CRUD operations for the defined dataset" and **FE-CRUD** for "standard CRUD
operations". The local Pokémon dataset therefore supports:

| Operation | Driven by |
|---|---|
| Read | HU-01, HU-02 (PokeAPI) and the local collection (HU-03) |
| Create | HU-03 (sync/import into the local store) |
| Update | HU-04 |
| Delete | **TR-API-CRUD / FE-CRUD** — a technical requirement, not a user story |

Delete is **not** a new user story and does not extend HU-01..HU-04; it is
traced to TR-API-CRUD and delivered in `005-hu04-local-update`, which owns the
lifecycle of local records. It follows the same rules as HU-04: protected
route, 404 for a missing record.
