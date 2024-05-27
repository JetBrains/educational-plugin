package com.jetbrains.edu.coursecreator.framework

/**
 * State of a task file in the context of syncing changes, which will be displayed in the project view
 * * [SyncChangesTaskFileState.INFO] - The changes for this file are not synchronized, but it does not break the structure of framework lesson
 * (the corresponding file is present in the next task). In this case, the information icon will be displayed
 *
 * * [SyncChangesTaskFileState.WARNING] - There is no corresponding file in the next task of the framework lesson.
 * In this case, the warning icon will be displayed
 */
enum class SyncChangesTaskFileState {
  INFO,
  WARNING
}