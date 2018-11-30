package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.addDefaultTaskDescription
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.placeholderDependencies
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import icons.EducationalCoreIcons
import java.io.IOException
import java.util.*
import kotlin.collections.LinkedHashMap

class CCCreateTask : CCCreateStudyItemActionBase<Task>(StudyItemType.TASK, EducationalCoreIcons.Task) {

  override fun addItem(course: Course, item: Task) {
    item.lesson.addTask(item)

    StepikCourseChangeHandler.contentChanged(item.lesson)
  }

  override fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem> =
    Function { file -> (item as? Task)?.lesson?.getTask(file.name) }

  override fun createItemDir(project: Project, item: Task,
                             parentDirectory: VirtualFile, course: Course): VirtualFile? {
    val configurator = course.configurator
    if (configurator == null) {
      LOG.info("Failed to get configurator for " + course.languageID)
      return null
    }
    return configurator.courseBuilder.createTaskContent(project, item, parentDirectory)
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

  override fun createAndInitItem(project: Project, course: Course, parentItem: StudyItem?, info: NewStudyItemInfo): Task? {
    if (parentItem !is Lesson) return null
    val newTask = EduTask(info.name)
    newTask.index = info.index
    newTask.lesson = parentItem
    newTask.addDefaultTaskDescription()
    if (parentItem is FrameworkLesson) {
      val prevTask = parentItem.getTaskList().getOrNull(info.index - 2)
      val prevTaskDir = prevTask?.getTaskDir(project)
      if (prevTask == null || prevTaskDir == null) {
        initTask(course, parentItem, newTask, info)
        return newTask
      }
      FileDocumentManager.getInstance().saveAllDocuments()
      // We can't just copy text from course objects because they can contain outdated text
      // in reason that we don't synchronize them with files system
      // So we need to load actual files text from filesystem
      val newTaskFiles = LinkedHashMap<String, TaskFile>()
      val testDirs = course.testDirs
      val defaultTestFileName = course.configurator?.testFileName ?: ""
      for ((path, file) in prevTask.taskFiles) {
        // We don't want to copy test files
        if (testDirs.none { path.startsWith(it) } && path != defaultTestFileName) {
          newTaskFiles[path] = file.copyForNewTask(prevTaskDir, newTask)
        }
      }
      newTask.taskFiles = newTaskFiles

      // If we insert new task between `task1` and `task2`
      // we should change target of all placeholder dependencies of `task2` from task file of `task1`
      // to the corresponding task file in new task
      parentItem.getTaskList().getOrNull(info.index - 1)
        ?.placeholderDependencies
        ?.forEach { dependency ->
          if (dependency.resolve(course)?.taskFile?.task == prevTask) {
            val placeholder = dependency.answerPlaceholder
            placeholder.placeholderDependency = dependency.copy(taskName = newTask.name)
          }
        }
    } else {
      initTask(course, parentItem, newTask, info)
    }
    return newTask
  }

  private fun initTask(course: Course, lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    if (!course.isStudy) {
      course.configurator?.courseBuilder?.initNewTask(lesson, task, info)
    }
  }

  private fun TaskFile.copyForNewTask(taskDir: VirtualFile, newTask: Task): TaskFile {
    val newTaskFile = TaskFile()
    newTaskFile.task = newTask
    newTaskFile.name = name
    val text = try {
      EduUtils.findTaskFileInDir(this, taskDir)?.let(CCUtils::loadText) ?: ""
    } catch (e: IOException) {
      LOG.error("Can't load text for `$name` task file", e)
      ""
    }
    newTaskFile.setText(text)
    newTaskFile.isVisible = isVisible
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
    newPlaceholder.placeholderDependency = AnswerPlaceholderDependency(newPlaceholder, sectionName, lesson.name, task.name, taskFile.name, index, false)
    return newPlaceholder
  }

  private fun AnswerPlaceholderDependency.copy(
    answerPlaceholder: AnswerPlaceholder = this.answerPlaceholder,
    sectionName: String? = this.sectionName,
    lessonName: String = this.lessonName,
    taskName: String = this.taskName,
    fileName: String = this.fileName,
    placeholderIndex: Int = this.placeholderIndex,
    isVisible: Boolean = this.isVisible
  ): AnswerPlaceholderDependency =
    AnswerPlaceholderDependency(answerPlaceholder, sectionName, lessonName, taskName, fileName, placeholderIndex, isVisible)

  companion object {
    private val LOG: Logger = Logger.getInstance(CCCreateTask::class.java)
  }
}
