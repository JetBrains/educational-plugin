package com.jetbrains.edu.learning.framework.impl

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.io.IOException

class FrameworkLessonManagerImpl(private val project: Project) : FrameworkLessonManager {

  private val storage: FrameworkStorage = FrameworkStorage(constructStoragePath(project))

  override fun prepareNextTask(lesson: FrameworkLesson, taskDir: VirtualFile, showDialogIfConflict: Boolean) {
    applyTargetTaskChanges(lesson, 1, taskDir, showDialogIfConflict)
  }

  override fun preparePrevTask(lesson: FrameworkLesson, taskDir: VirtualFile, showDialogIfConflict: Boolean) {
    applyTargetTaskChanges(lesson, -1, taskDir, showDialogIfConflict)
  }

  override fun saveExternalChanges(task: Task, externalState: Map<String, String>) {
    require(EduUtils.isStudentProject(project)) {
      "`saveExternalChanges` should be called only if course in study mode"
    }
    require(task.lesson is FrameworkLesson) {
      "Only solutions of framework tasks can be saved"
    }

    val changes = calculateChanges(task.allFiles, externalState)
    val currentRecord = task.record
    task.record = try {
      storage.updateUserChanges(currentRecord, changes)
    } catch (e: IOException) {
      LOG.error("Failed to save solution for task `${task.name}`", e)
      currentRecord
    }
  }

  private fun applyTargetTaskChanges(
    lesson: FrameworkLesson,
    taskIndexDelta: Int,
    taskDir: VirtualFile,
    showDialogIfConflict: Boolean
  ) {
    require(EduUtils.isStudentProject(project)) {
      "`applyTargetTaskChanges` should be called only if course in study mode"
    }
    val currentTaskIndex = lesson.currentTaskIndex
    val targetTaskIndex = currentTaskIndex + taskIndexDelta

    val currentTask = lesson.taskList[currentTaskIndex]
    val targetTask = lesson.taskList[targetTaskIndex]

    lesson.currentTaskIndex = targetTaskIndex

    val currentRecord = currentTask.record
    val targetRecord = targetTask.record

    val initialCurrentFiles = currentTask.allFiles
    val (newCurrentRecord, currentUserChanges) = try {
      updateUserChanges(currentRecord, initialCurrentFiles, taskDir)
    } catch (e: IOException) {
      LOG.error("Failed to save user changes for task `${currentTask.name}`", e)
      UpdatedUserChanges(currentRecord, UserChanges.empty())
    }

    currentTask.record = newCurrentRecord

    val nextUserChanges = try {
      storage.getUserChanges(targetRecord)
    } catch (e: IOException) {
      LOG.error("Failed to get user changes for task `${currentTask.name}`", e)
      UserChanges.empty()
    }

    val currentState = HashMap(initialCurrentFiles).apply { currentUserChanges.apply(this) }
    val targetState = HashMap(targetTask.allFiles).apply { nextUserChanges.apply(this) }

    // There are special rules for hyperskill courses for now
    // All user changes from current task should be propagated to next task as is
    val changes = if (taskIndexDelta == 1 && lesson.course is HyperskillCourse) {
      calculateHypeskillChanges(lesson, targetState, currentState, targetTask, showDialogIfConflict)
    } else {
      calculateChanges(currentState, targetState)
    }

    changes.apply(project, taskDir, targetTask)
  }

  private fun calculateHypeskillChanges(
    lesson: FrameworkLesson,
    targetState: Map<String, String>,
    currentState: Map<String, String>,
    targetTask: Task,
    showDialogIfConflict: Boolean
  ): UserChanges {
    val (currentTaskFilesState, currentTestFilesState) = currentState.split(lesson)
    val (targetTaskFiles, targetTestFiles) = targetState.split(lesson)

    // Creates [Change]s to propagates all current changes of task files to target task.
    // Technically, we won't change text of task files, just add user created task files to target task
    fun calculateCurrentTaskChanges(): UserChanges {
      val newTaskFilesChanges = (currentTaskFilesState - targetTaskFiles.keys).map { (path, text) ->
        Change.AddUserCreatedTaskFile(path, text)
      }
      val testChanges = calculateChanges(currentTestFilesState, targetTestFiles)
      return testChanges + newTaskFilesChanges
    }
    
    // target task initialization
    return if (targetTask.record == -1) {
      calculateCurrentTaskChanges()
    } else {
      if (currentTaskFilesState == targetTaskFiles) {
        // if current and target states of task files are the same
        // it needs to calculate only diff for test files
        calculateChanges(currentTestFilesState, targetTestFiles)
      } else {
        val result = if (showDialogIfConflict) {
          Messages.showYesNoDialog(project, "The current task changes conflict with next task. Replace with current changes?",
                                   "Changes conflict", "Replace", "Keep", null)
        } else {
          Messages.NO
        }
        if (result == Messages.NO) {
          calculateChanges(currentState, targetState)
        } else {
          calculateCurrentTaskChanges()
        }
      }
    }
  }

  private fun updateUserChanges(record: Int, initialFiles: Map<String, String>, taskDir: VirtualFile): UpdatedUserChanges {
    val documentManager = FileDocumentManager.getInstance()
    val currentState = HashMap<String, String>()
    for ((path, _) in initialFiles) {
      val file = taskDir.findFileByRelativePath(path) ?: continue
      currentState[path] = runReadAction { documentManager.getDocument(file)?.text } ?: continue
    }
    val userChanges = calculateChanges(initialFiles, currentState)
    return updateUserChanges(record, userChanges)
  }

  @Synchronized
  private fun updateUserChanges(record: Int, changes: UserChanges): UpdatedUserChanges {
    return try {
      val newRecord = storage.updateUserChanges(record, changes)
      storage.force()
      UpdatedUserChanges(newRecord, changes)
    } catch (e: IOException) {
      LOG.error("Failed to update user changes", e)
      UpdatedUserChanges(record, UserChanges.empty())
    }
  }

  private fun calculateChanges(
    currentState: Map<String, String>,
    targetState: Map<String, String>
  ): UserChanges {
    val changes = mutableListOf<Change>()
    val current = HashMap(currentState)
    for ((path, nextText) in targetState) {
      val currentText = current.remove(path)
      changes += if (currentText == null) {
        Change.AddFile(path, nextText)
      } else {
        Change.ChangeFile(path, nextText)
      }
    }

    current.mapTo(changes) { Change.RemoveFile(it.key) }
    return UserChanges(changes)
  }

  private val Task.allFiles: Map<String, String> get() = taskFiles.mapValues { it.value.text }

  private fun Map<String, String>.split(lesson: FrameworkLesson): Pair<Map<String, String>, Map<String, String>> {
    val testDirs = lesson.course.testDirs
    val defaultTestName = lesson.course.configurator?.testFileName ?: ""
    val taskFiles = HashMap<String, String>()
    val testFiles = HashMap<String, String>()

    for ((path, text) in this) {
      val state = if (path == defaultTestName || testDirs.any { path.startsWith(it) }) {
        testFiles
      } else {
        taskFiles
      }
      state[path] = text
    }

    return taskFiles to testFiles
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(FrameworkLessonManagerImpl::class.java)

    @VisibleForTesting
    fun constructStoragePath(project: Project): String =
      FileUtil.join(project.basePath!!, Project.DIRECTORY_STORE_FOLDER, "frameworkLessonHistory", "storage")
  }
}

private data class UpdatedUserChanges(
  val record: Int,
  val changes: UserChanges
)
