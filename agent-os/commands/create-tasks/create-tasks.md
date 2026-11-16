I want you to create a tasks breakdown from a given spec and requirements for a new feature using the following MULTI-PHASE process and instructions.

Carefully read and execute the instructions in the following files IN SEQUENCE, following their numbered file names. Only proceed to the next numbered instruction file once the previous numbered instruction has been executed.

Instructions to follow in sequence:

# PHASE 1: Get Spec Requirements

The FIRST STEP is to make sure you have ONE OR BOTH of these files to inform your tasks breakdown:

- `agent-os/specs/[this-spec]/spec.md`
- `agent-os/specs/[this-spec]/planning/requirements.md`

IF you don't have ONE OR BOTH of those files in your current conversation context, then ask user to provide direction on where to you can find them by outputting the following request then wait for user's response:

"I'll need a spec.md or requirements.md (or both) in order to build a tasks list.

Please direct me to where I can find those. If you haven't created them yet, you can run /shape-spec or /write-spec."

# PHASE 2: Create Tasks List

Now that you have the spec.md AND/OR requirements.md, please break those down into an actionable tasks list with strategic grouping and ordering, by following these instructions:

# Task List Creation

## Core Responsibilities

1. **Analyze spec and requirements**: Read and analyze the spec.md and/or requirements.md to inform the tasks list you will create.
2. **Plan task execution order**: Break the requirements into a list of tasks in an order that takes their dependencies into account.
3. **Group tasks by specialization**: Group tasks that require the same skill or layer together (data layer, use cases/domain, ViewModel & UI, etc.)
4. **Create Tasks list**: Create the markdown tasks list broken into groups with sub-tasks.

## Workflow

### Step 1: Analyze Spec & Requirements

Read each of these files (whichever are available) and analyze them to understand the requirements for this feature implementation:

- `agent-os/specs/[this-spec]/spec.md`
- `agent-os/specs/[this-spec]/planning/requirements.md`

Use your learnings to inform the tasks list and groupings you will create in the next step.

### Step 2: Create Tasks Breakdown

Generate `agent-os/specs/[current-spec]/tasks.md`.

**Important**: The exact tasks, task groups, and organization will vary based on the feature's specific requirements. The following is an example format - adapt the content of the tasks list to match what THIS feature actually needs.

```markdown
# Task Breakdown: [Feature Name]

## Overview

Total Tasks: [count]

## Task List

### Data Layer

#### Task Group 1: Models & Data Sources

**Dependencies:** None

- [ ] 1.0 Complete data layer (in `data-source` / `core`)
  - [ ] 1.1 Write 2-8 focused tests for [model/mapper] functionality
    - Limit to 2-8 highly focused tests maximum
    - Test only critical behaviors (e.g., key mapping, parse of a critical response, core transformation)
    - Skip exhaustive coverage of all fields and edge cases
  - [ ] 1.2 Create [Model] data class / POJO
    - Fields: [list]
    - One class per file; sealed result class for the use case step if applicable
    - Reuse pattern from: [existing model if applicable]
  - [ ] 1.3 Add Retrofit service method(s) for [endpoint]
    - Suspend function returning a typed result
    - Follow pattern from: [existing service]
  - [ ] 1.4 Wire local storage if needed
    - Realm schema (+ migration if changing an existing schema) / DataStore / EncryptedPreferences (never PreferencesManager)
  - [ ] 1.5 Ensure data layer tests pass
    - Run ONLY the 2-8 tests written in 1.1
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**

- The 2-8 tests written in 1.1 pass
- Models/POJOs follow conventions (one class per file)
- Network/storage access returns typed results
- Realm schema changes include a migration

### Domain Layer

#### Task Group 2: Use Cases

**Dependencies:** Task Group 1

- [ ] 2.0 Complete domain layer
  - [ ] 2.1 Write 2-8 focused tests for use case(s)
    - Limit to 2-8 highly focused tests maximum
    - Test only critical logic (e.g., primary success path, key error result)
    - Skip exhaustive testing of all branches
  - [ ] 2.2 Create [Feature]UseCase (single responsibility)
    - Inject dependencies via Hilt `@Inject constructor`
    - Return a dedicated sealed result class
    - Never pass `Activity` — split into request/complete steps if a prompt is needed
  - [ ] 2.3 Add Hilt bindings if manual provision is required
    - `@Provides` in `AppModule` (DI modules live in `app/src/main/java/di/`)
  - [ ] 2.4 Handle errors and logging
    - Convert exceptions to typed results at this boundary
    - `Firebase.crashlytics.recordException(e)` + `SnappLog.log(...)`
  - [ ] 2.5 Ensure domain layer tests pass
    - Run ONLY the 2-8 tests written in 2.1
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**

- The 2-8 tests written in 2.1 pass
- Business logic lives in use cases, not the UI
- Outcomes modeled with sealed result classes
- Dependencies injected via Hilt

### UI Layer

#### Task Group 3: ViewModel & Screen

**Dependencies:** Task Group 2

- [ ] 3.0 Complete UI layer
  - [ ] 3.1 Write 2-8 focused tests for the ViewModel
    - Limit to 2-8 highly focused tests maximum
    - Test only critical behavior (e.g., state emitted on success, one-shot event on error)
    - Skip exhaustive testing of all states
  - [ ] 3.2 Create/extend [Feature]ViewModel
    - Expose state via `StateFlow`; one-shot events via `Channel`/`SharedFlow`
    - Invoke use cases from Task Group 2
  - [ ] 3.3 Build the screen
    - **Views path:** Fragment extending `BaseFragment<Binding>` (use lifecycle hooks); ViewBinding; XML layout
    - **Compose path:** stateless composables + Material3 theme; collect state with `collectAsStateWithLifecycle()`
    - Match mockup: `planning/visuals/[file]`
  - [ ] 3.4 Wire input validation & user feedback
    - Validate at the ViewModel boundary; localized messages from `strings.xml`
  - [ ] 3.5 Apply styling & layout
    - Resources for colors/dimens/strings; RTL-aware; touch targets ≥48dp
  - [ ] 3.6 Ensure UI tests pass
    - Run ONLY the 2-8 tests written in 3.1
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**

- The 2-8 tests written in 3.1 pass
- Screen renders correctly on an emulator/device
- State/events flow through the ViewModel
- Matches visual design and supports RTL

### Testing

#### Task Group 4: Test Review & Gap Analysis

**Dependencies:** Task Groups 1-3

- [ ] 4.0 Review existing tests and fill critical gaps only
  - [ ] 4.1 Review tests from Task Groups 1-3
    - Review the 2-8 tests written for the data layer (Task 1.1)
    - Review the 2-8 tests written for the use cases (Task 2.1)
    - Review the 2-8 tests written for the ViewModel/UI (Task 3.1)
    - Total existing tests: approximately 6-24 tests
  - [ ] 4.2 Analyze test coverage gaps for THIS feature only
    - Identify critical user workflows that lack test coverage
    - Focus ONLY on gaps related to this spec's feature requirements
    - Do NOT assess entire application test coverage
    - Prioritize end-to-end workflows over unit test gaps
  - [ ] 4.3 Write up to 10 additional strategic tests maximum
    - Add maximum of 10 new tests to fill identified critical gaps
    - Focus on integration points and end-to-end workflows
    - Do NOT write comprehensive coverage for all scenarios
    - Skip edge cases, performance tests, and accessibility tests unless business-critical
  - [ ] 4.4 Run feature-specific tests only
    - Run ONLY tests related to this spec's feature (tests from 1.1, 2.1, 3.1, and 4.3)
    - Expected total: approximately 16-34 tests maximum
    - Do NOT run the entire application test suite
    - Verify critical workflows pass

**Acceptance Criteria:**

- All feature-specific tests pass (approximately 16-34 tests total)
- Critical user workflows for this feature are covered
- No more than 10 additional tests added when filling in testing gaps
- Testing focused exclusively on this spec's feature requirements

## Execution Order

Recommended implementation sequence:

1. Data Layer (Task Group 1)
2. Domain Layer / Use Cases (Task Group 2)
3. UI Layer — ViewModel & Screen (Task Group 3)
4. Test Review & Gap Analysis (Task Group 4)
```

**Note**: Adapt this structure based on the actual feature requirements. Some features may need:

- Different task groups (e.g., email notifications, payment processing, data migration)
- Different execution order based on dependencies
- More or fewer sub-tasks per group

## Important Constraints

- **Create tasks that are specific and verifiable**
- **Group related tasks:** For example, group data-layer tasks together and ViewModel/UI tasks together, following the app's module boundaries (`data-source`, `core`, feature modules, `app`).
- **Limit test writing during development**:
  - Each task group (1-3) should write 2-8 focused tests maximum
  - Tests should cover only critical behaviors, not exhaustive coverage
  - Test verification should run ONLY the newly written tests, not the entire suite
  - If there is a dedicated test coverage group for filling in gaps in test coverage, this group should add only a maximum of 10 additional tests IF NECESSARY to fill critical gaps
- **Use a focused test-driven approach** where each task group starts with writing 2-8 tests (x.1 sub-task) and ends with running ONLY those tests (final sub-task)
- **Include acceptance criteria** for each task group
- **Reference visual assets** if visuals are available

## Display confirmation and next step

Display the following message to the user:

```
The tasks list has created at `agent-os/specs/[this-spec]/tasks.md`.

Review it closely to make sure it all looks good.

NEXT STEP 👉 Run `/implement-tasks` (simple, effective) or `/orchestrate-tasks` (advanced, powerful) to start building!
```

## User Standards & Preferences Compliance

IMPORTANT: Ensure that the tasks list is ALIGNED and DOES NOT CONFLICT with the user's preferences and standards as detailed in the following files:

@agent-os/standards/data/networking.md
@agent-os/standards/data/data-models.md
@agent-os/standards/data/local-storage.md
@agent-os/standards/ui/ui-components.md
@agent-os/standards/ui/styling.md
@agent-os/standards/ui/layouts.md
@agent-os/standards/ui/accessibility.md
@agent-os/standards/global/coding-style.md
@agent-os/standards/global/commenting.md
@agent-os/standards/global/conventions.md
@agent-os/standards/global/error-handling.md
@agent-os/standards/global/tech-stack.md
@agent-os/standards/global/validation.md
@agent-os/standards/testing/test-writing.md
