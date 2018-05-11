package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import icons.EducationalCoreIcons
import java.io.IOException

import java.util.Collections

open class CCCreateTask : CCCreateStudyItemActionBase<Task>(EduNames.TASK, EducationalCoreIcons.Task) {

  override fun addItem(course: Course, item: Task) {
    item.lesson.addTask(item)
  }

  override fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem> =
    Function { file -> (item as? Task)?.lesson?.getTask(file.name) }

  override fun createItemDir(project: Project, item: Task,
                             parentDirectory: VirtualFile, course: Course): VirtualFile? {
    val configurator = EduConfiguratorManager.forLanguage(course.languageById!!)
    return configurator?.courseBuilder?.createTaskContent(project, item, parentDirectory, course)
  }

  override fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int =
    (parentItem as? Lesson)?.getTaskList()?.size ?: 0

  override fun getParentItem(course: Course, directory: VirtualFile): StudyItem? {
    val task = EduUtils.getTask(directory, course) ?: return EduUtils.getLesson(directory, course)
    return task.lesson
  }

  override fun getThresholdItem(course: Course, sourceDirectory: VirtualFile): StudyItem? =
    EduUtils.getTask(sourceDirectory, course)

  override fun isAddedAsLast(sourceDirectory: VirtualFile, project: Project, course: Course): Boolean =
    EduUtils.getLesson(sourceDirectory, course) != null

  override fun sortSiblings(course: Course, parentItem: StudyItem?) {
    if (parentItem is Lesson) {
      Collections.sort(parentItem.getTaskList(), EduUtils.INDEX_COMPARATOR)
    }
  }

  override fun createAndInitItem(project: Project, course: Course, parentItem: StudyItem?, name: String, index: Int): Task? {
    if (parentItem !is Lesson) return null
    val newTask = EduTask(name)
    newTask.index = index
    newTask.lesson = parentItem
    newTask.addDefaultTaskDescription()
    if (parentItem is FrameworkLesson) {
      val prevTask = parentItem.getTaskList().getOrNull(index - 2) ?: return newTask
      val prevTaskDir = prevTask.getTaskDir(project) ?: return newTask
      FileDocumentManager.getInstance().saveAllDocuments()
      newTask.taskFiles = prevTask.taskFiles.mapValues { (_, taskFile) -> taskFile.copyForNewTask(prevTaskDir, newTask) }
    }
    return newTask
  }

  private fun TaskFile.copyForNewTask(taskDir: VirtualFile, newTask: Task): TaskFile {
    val newTaskFile = TaskFile()
    newTaskFile.task = newTask
    newTaskFile.name = name
    newTaskFile.text = try {
      EduUtils.findTaskFileInDir(this, taskDir)?.let { VfsUtil.loadText(it) } ?: ""
    } catch (e: IOException) {
      LOG.error("Can't load text for `$name` task file", e)
      ""
    }
    newTaskFile.answerPlaceholders = answerPlaceholders.map { it.copyForNewTaskFile(newTaskFile) }
    return newTaskFile
  }

  private fun AnswerPlaceholder.copyForNewTaskFile(newTaskFile: TaskFile): AnswerPlaceholder {
    val newPlaceholder = AnswerPlaceholder()
    newPlaceholder.taskFile = newTaskFile
    newPlaceholder.placeholderText = placeholderText
    newPlaceholder.offset = offset
    newPlaceholder.length = length
    newPlaceholder.possibleAnswer = possibleAnswer
    newPlaceholder.index = index
    newPlaceholder.hints = ArrayList(hints)
    newPlaceholder.useLength = useLength
    val state = initialState
    if (state != null) {
      newPlaceholder.initialState = AnswerPlaceholder.MyInitialState(state.offset, state.length)
    }
    val taskFile = taskFile
    val task = taskFile.task
    val lesson = task.lesson
    val sectionName = (lesson.container as? Section)?.name
    newPlaceholder.placeholderDependency = AnswerPlaceholderDependency(newPlaceholder, sectionName, lesson.name, task.name, taskFile.name, index)
    return newPlaceholder
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCCreateTask::class.java)
  }
}
