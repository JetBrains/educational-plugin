package com.jetbrains.edu.learning.framework.impl

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.storage.AbstractStorage
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.isToEncodeContent
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.apache.commons.codec.binary.Base64
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Keeps list of [Change]s for each task. Change list is difference between initial task state and latest one.
 *
 * Allows navigating between tasks in framework lessons in learner mode (where only current task is visible for a learner)
 * without rewriting whole task content.
 * It can be essential in large projects like Android applications where a lot of files are the same between two consecutive tasks
 */
class FrameworkLessonManagerImpl(private val project: Project) : FrameworkLessonManager, Disposable {

  @VisibleForTesting
  var storage: FrameworkStorage = createStorage(project)

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

    val taskFiles = task.allFiles.split(task.course).first
    val externalTaskFiles = externalState.split(task.course).first
    val changes = calculateChanges(taskFiles, externalTaskFiles)
    val currentRecord = task.record
    task.record = try {
      storage.updateUserChanges(currentRecord, changes)
    }
    catch (e: IOException) {
      LOG.error("Failed to save solution for task `${task.name}`", e)
      currentRecord
    }
    YamlFormatSynchronizer.saveItem(task)
  }

  override fun updateUserChanges(task: Task, newInitialState: Map<String, String>) {
    require(EduUtils.isStudentProject(project)) {
      "`updateUserChanges` should be called only if course in study mode"
    }
    require(task.lesson is FrameworkLesson) {
      "Only solutions of framework tasks can be saved"
    }

    val currentRecord = task.record
    if (currentRecord == -1) return

    val changes = try {
      storage.getUserChanges(currentRecord)
    }
    catch (e: IOException) {
      LOG.error("Failed to get user changes for task `${task.name}`", e)
      return
    }

    val newChanges = changes.changes.mapNotNull {
      when (it) {
        is Change.AddFile -> if (it.path in newInitialState) Change.ChangeFile(it.path, it.text) else it
        is Change.RemoveFile -> if (it.path !in newInitialState) null else it
        is Change.ChangeFile -> if (it.path !in newInitialState) Change.AddFile(it.path, it.text) else it
        is Change.PropagateLearnerCreatedTaskFile,
        is Change.RemoveTaskFile -> it
      }
    }

    try {
      storage.updateUserChanges(currentRecord, UserChanges(newChanges))
    }
    catch (e: IOException) {
      LOG.error("Failed to update user changes for task `${task.name}`", e)
    }
  }

  override fun getChangesTimestamp(task: Task): Long {
    require(EduUtils.isStudentProject(project)) {
      "`getTimestamp` should be called only if course in study mode"
    }
    require(task.lesson is FrameworkLesson) {
      "Changes timestamp makes sense only for framework tasks"
    }

    return storage.getUserChanges(task.record).timestamp
  }

  /**
   * Convert the current state on local FS related to current task in framework lesson
   * to a new one, to get state of next/previous (target) task.
   */
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
    YamlFormatSynchronizer.saveItem(lesson)

    val currentRecord = currentTask.record
    val targetRecord = targetTask.record

    val initialCurrentFiles = currentTask.allFiles

    // 1. Get difference between initial state of current task and previous task state
    // and construct previous state of current task.
    // Previous state is needed to determine if a user made any new change
    val previousCurrentUserChanges = getUserChanges(currentTask)
    val previousCurrentState = HashMap(initialCurrentFiles).apply { previousCurrentUserChanges.apply(this) }

    // 2. Calculate difference between initial state of current task and current state on local FS.
    // Update change list for current task in [storage] to have ability to restore state of current task in future
    val (newCurrentRecord, currentUserChanges) = try {
      updateUserChanges(currentRecord, initialCurrentFiles, taskDir)
    }
    catch (e: IOException) {
      LOG.error("Failed to save user changes for task `${currentTask.name}`", e)
      UpdatedUserChanges(currentRecord, UserChanges.empty())
    }

    // 3. Update record index to a new one.
    currentTask.record = newCurrentRecord
    YamlFormatSynchronizer.saveItem(currentTask)

    // 4. Get difference (change list) between initial and latest states of target task
    val nextUserChanges = getUserChanges(targetTask)

    // 5. Apply change lists to initial state to get latest states of current and target tasks
    val currentState = HashMap(initialCurrentFiles).apply { currentUserChanges.apply(this) }
    val targetState = HashMap(targetTask.allFiles).apply { nextUserChanges.apply(this) }

    // 6. Calculate difference between latest states of current and target tasks
    // Note, there are special rules for hyperskill courses for now
    // All user changes from the current task should be propagated to next task as is

    // If a user navigated back to current task, didn't make any change and wants to navigate to next task again
    // we shouldn't try to propagate current changes to next task
    val currentTaskHasNewUserChanges = !(currentRecord != -1 && targetRecord != -1 && previousCurrentState == currentState)

    val course = lesson.course
    val isNonTemplateBased = !lesson.isTemplateBased || course is HyperskillCourse && !course.isTemplateBased

    val changes = if (currentTaskHasNewUserChanges && taskIndexDelta == 1 && isNonTemplateBased) {
      calculatePropagationChanges(targetTask, currentTask, currentState, targetState, showDialogIfConflict)
    }
    else {
      calculateChanges(currentState, targetState)
    }

    // 7. Apply difference between latest states of current and target tasks on local FS
    changes.apply(project, taskDir, targetTask)
    YamlFormatSynchronizer.saveItem(targetTask)
  }

  private fun getUserChanges(task: Task): UserChanges {
    return try {
      storage.getUserChanges(task.record)
    }
    catch (e: IOException) {
      LOG.error("Failed to get user changes for task `${task.name}`", e)
      UserChanges.empty()
    }
  }

  /**
   * Returns [Change]s to propagate user changes from [currentState] to [targetTask].
   *
   * In case, when it's impossible due to simultaneous incompatible user changes in [currentState] and [targetState],
   * it asks user to choose what change he wants to apply.
   */
  private fun calculatePropagationChanges(
    targetTask: Task,
    currentTask: Task,
    currentState: Map<String, String>,
    targetState: Map<String, String>,
    showDialogIfConflict: Boolean
  ): UserChanges {
    val (currentTaskFilesState, currentTestFilesState) = currentState.split(targetTask.course)
    val (targetTaskFiles, targetTestFiles) = targetState.split(targetTask.course)

    // Creates [Change]s to propagates all current changes of task files to target task.
    // Technically, we won't change text of task files, just add/remove user created/removed task files to/from target task
    fun calculateCurrentTaskChanges(): UserChanges {
      val toRemove = HashMap(targetTaskFiles)
      val taskFileChanges = mutableListOf<Change>()

      for ((path, text) in currentTaskFilesState) {
        val targetText = toRemove.remove(path)
        if (targetText == null) {
          taskFileChanges += Change.PropagateLearnerCreatedTaskFile(path, text)
        }
      }

      for ((path, _) in toRemove) {
        taskFileChanges += Change.RemoveTaskFile(path)
      }
      val testChanges = calculateChanges(currentTestFilesState, targetTestFiles)
      return testChanges + taskFileChanges
    }

    // target task initialization
    if (targetTask.record == -1) {
      return calculateCurrentTaskChanges()
    }
    if (currentTaskFilesState == targetTaskFiles) {
      // if current and target states of task files are the same
      // it needs to calculate only diff for test files
      return calculateChanges(currentTestFilesState, targetTestFiles)
    }

    val keepConflictingChanges = if (showDialogIfConflict) {
      val currentTaskName = "${currentTask.uiName} ${currentTask.index}"
      val targetTaskName = "${targetTask.uiName} ${targetTask.index}"
      val message = "Changes from $currentTaskName conflict with the changes made on $targetTaskName.\n" +
                    "Keep content of $targetTaskName or replace with the changes from $currentTaskName?"
      Messages.showYesNoDialog(project, message, "Conflicting Changes", "Keep", "Replace", null)
    }
    else {
      Messages.YES
    }

    return if (keepConflictingChanges == Messages.YES) {
      calculateChanges(currentState, targetState)
    }
    else {
      calculateCurrentTaskChanges()
    }
  }

  private fun updateUserChanges(record: Int, initialFiles: Map<String, String>, taskDir: VirtualFile): UpdatedUserChanges {
    val documentManager = FileDocumentManager.getInstance()
    val currentState = HashMap<String, String>()
    for ((path, _) in initialFiles) {
      val file = taskDir.findFileByRelativePath(path) ?: continue

      val text = if (file.isToEncodeContent())
        Base64.encodeBase64String(file.contentsToByteArray())
      else runReadAction { documentManager.getDocument(file)?.text }

      if (text == null) {
        continue
      }

      currentState[path] = text
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
    }
    catch (e: IOException) {
      LOG.error("Failed to update user changes", e)
      UpdatedUserChanges(record, UserChanges.empty())
    }
  }

  /**
   * Returns [Change]s to convert [currentState] to [targetState]
   */
  private fun calculateChanges(
    currentState: Map<String, String>,
    targetState: Map<String, String>
  ): UserChanges {
    val changes = mutableListOf<Change>()
    val current = HashMap(currentState)
    loop@ for ((path, nextText) in targetState) {
      val currentText = current.remove(path)
      changes += when {
        currentText == null -> Change.AddFile(path, nextText)
        currentText != nextText -> Change.ChangeFile(path, nextText)
        else -> continue@loop
      }
    }

    current.mapTo(changes) { Change.RemoveFile(it.key) }
    return UserChanges(changes)
  }

  private val Task.allFiles: Map<String, String> get() = taskFiles.mapValues { it.value.text }

  private fun Map<String, String>.split(course: Course): Pair<Map<String, String>, Map<String, String>> {
    val testDirs = course.testDirs
    val defaultTestName = course.configurator?.testFileName ?: ""
    val taskFiles = HashMap<String, String>()
    val testFiles = HashMap<String, String>()

    for ((path, text) in this) {
      val state = if (path == defaultTestName || testDirs.any { path.startsWith(it) }) {
        testFiles
      }
      else {
        taskFiles
      }
      state[path] = text
    }

    return taskFiles to testFiles
  }

  override fun dispose() {
    Disposer.dispose(storage)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(FrameworkLessonManagerImpl::class.java)

    const val VERSION: Int = 1

    @VisibleForTesting
    fun constructStoragePath(project: Project): Path =
      Paths.get(FileUtil.join(project.basePath!!, Project.DIRECTORY_STORE_FOLDER, "frameworkLessonHistory", "storage"))

    @VisibleForTesting
    fun createStorage(project: Project): FrameworkStorage {
      val storageFilePath = constructStoragePath(project)
      val storage = FrameworkStorage(storageFilePath)
      return try {
        storage.migrate(VERSION)
        storage
      }
      catch (e: IOException) {
        LOG.error(e)
        AbstractStorage.deleteFiles(storageFilePath.toString())
        FrameworkStorage(storageFilePath, VERSION)
      }
    }
  }
}

private data class UpdatedUserChanges(
  val record: Int,
  val changes: UserChanges
)
