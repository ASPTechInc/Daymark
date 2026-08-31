# API Reference & Component Documentation

This document provides a high-level overview of the core components and logic within the Daymark application.

---

## UI Components

### [TagSelectionAdapter](/app/src/main/java/com/asptechinc/daymark/ui/TagSelectionAdapter.kt)
Adapter for selecting tags in a searchable list. It uses `ListAdapter` for efficient updates and maintains the selection state internally.

- **`filter(query: String)`**: Filters the tag list based on a case-insensitive search string.
- **`getSelectedIds()`**: Returns a list of IDs for all tags currently checked in the UI.

### [ActivityAdapter](/app/src/main/java/com/asptechinc/daymark/ActivityAdapter.kt)
The primary adapter for the main activity list.
- **Efficient Updates**: Uses `ListAdapter` and `DiffUtil` to animate changes.
- **Drag & Drop**: Supports manual reordering when drag mode is enabled.
- **Custom Content Diffing**: Ignores the `position` field during content comparison to prevent redundant animations after a manual drag operation.

---

## Business Logic

### [MainViewModel](/app/src/main/java/com/asptechinc/daymark/ui/MainViewModel.kt)
Coordinates data between repositories and the main UI.

#### Filtering and Sorting (`filterAndSort`)
The core logic for processing the activity list based on user preferences.
- **Search**: Case-insensitive matching on activity names.
- **Filters**: Supports category, single-tag, month, year, and status (archived/completed).
- **Sorting**:
    - **Name**: Alphabetical (A-Z or Z-A).
    - **Category**: Alphabetical by category name.
    - **Dates**: Chronological. Activities without end dates are prioritized based on the sort direction to ensure they appear consistently.
    - **Manual**: Respects the `position` stored in the database.

---

## Core Logic Patterns

### Smooth Reordering
To ensure a glitch-free drag-and-drop experience:
1. **Local State**: A `temporaryList` in `MainActivity` tracks movements immediately via `submitList()`.
2. **Deferred Persistence**: The database is only updated when the user releases the item (`clearView`).
3. **Smart Diffing**: The `ActivityAdapter` ignores position changes when comparing "content", so the final sync from the database doesn't trigger a secondary "swap" animation.
