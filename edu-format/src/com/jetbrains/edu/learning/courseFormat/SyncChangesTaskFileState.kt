package com.jetbrains.edu.learning.courseFormat

/**
 * State of a task file in the context of syncing changes, which will be displayed in the project view
 *
 * * [SyncChangesTaskFileState.NONE] - The changes for this file are already synchronized, or this file is not available for synchronization.
 * In this case, the icons will not be displayed.
 *
 * * [SyncChangesTaskFileState.INFO] - The changes for this file are not synchronized, but it does not break the structure of framework lesson
 * (the corresponding file is present in the next task). In this case, the information icon will be displayed
 *
 * * [SyncChangesTaskFileState.WARNING] - There is no corresponding file in the next task of the framework lesson.
 * In this case, the warning icon will be displayed
 */
enum class SyncChangesTaskFileState {
  NONE,
  INFO,
  WARNING
}