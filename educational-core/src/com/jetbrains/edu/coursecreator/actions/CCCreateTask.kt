package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.messages.EduCoreActionBundle
import com.jetbrains.edu.learning.messages.EduCoreStudyItemBundle
import icons.EducationalCoreIcons.IdeTask
import icons.EducationalCoreIcons.Task
import java.util.*

class CCCreateTask : CCCreateStudyItemActionBase<Task>(StudyItemType.TASK, Task) {

  override fun addItem(course: Course, item: Task) {
    item.lesson.addTask(item)
  }

  override fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem> =
    Function { file -> (item as? Task)?.lesson?.getTask(file.name) }

  override fun createItemDir(project: Project, course: Course, item: Task, parentDirectory: VirtualFile): VirtualFile? {
    val configurator = course.configurator
    if (configurator == null) {
      LOG.info("Failed to get configurator for " + course.languageID)
      return null
    }
    return configurator.courseBuilder.createTaskContent(project, item, parentDirectory)
  }

  override fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int =
    (parentItem as? Lesson)?.taskList?.size ?: 0

  override fun getParentItem(
    project: Project,
    course: Course,
    directory: VirtualFile
  ): StudyItem? {
    val task = EduUtils.getTask(project, course, directory) ?: return EduUtils.getLesson(project, course, directory)
    return task.lesson
  }

  override fun getThresholdItem(project: Project, course: Course, sourceDirectory: VirtualFile): StudyItem? =
    EduUtils.getTask(project, course, sourceDirectory)

  override fun isAddedAsLast(project: Project, course: Course, sourceDirectory: VirtualFile): Boolean =
    EduUtils.getLesson(project, course, sourceDirectory) != null

  override fun sortSiblings(course: Course, parentItem: StudyItem?) {
    if (parentItem is Lesson) {
      parentItem.sortItems()
    }
  }

  override fun initItem(project: Project, course: Course, parentItem: StudyItem?, item: Task, info: NewStudyItemInfo) {
    require(parentItem is Lesson) {
      "parentItem should be Lesson, found `$parentItem`"
    }
    item.lesson = parentItem
    item.addDefaultTaskDescription()

    if (parentItem is FrameworkLesson) {
      val prevTask = parentItem.getTaskList().getOrNull(info.index - 2)
      val prevTaskDir = prevTask?.getTaskDir(project)
      if (prevTask == null || prevTaskDir == null) {
        initTask(project, course, item, info)
        return
      }
      FileDocumentManager.getInstance().saveAllDocuments()
      // We can't just copy text from course objects because they can contain outdated text
      // in reason that we don't synchronize them with files system
      // So we need to load actual files text from filesystem
      val newTaskFiles = LinkedHashMap<String, TaskFile>()
      val testDirs = course.testDirs
      val defaultTestFileName = course.configurator?.testFileName ?: ""
      val needCopyTests = CCSettings.getInstance().copyTestsInFrameworkLessons()
      for ((path, file) in prevTask.taskFiles) {
        if (needCopyTests || !(testDirs.any { path.startsWith(it) } || path == defaultTestFileName)) {
          newTaskFiles[path] = file.copyForNewTask(prevTaskDir, item)
        }
      }
      item.taskFiles = newTaskFiles

      if (!needCopyTests) {
        initTask(project, course, item, info, withSources = false)
      }

      // If we insert new task between `task1` and `task2`
      // we should change target of all placeholder dependencies of `task2` from task file of `task1`
      // to the corresponding task file in new task
      parentItem.getTaskList().getOrNull(info.index - 1)
        ?.placeholderDependencies
        ?.forEach { dependency ->
          if (dependency.resolve(course)?.taskFile?.task == prevTask) {
            val placeholder = dependency.answerPlaceholder
            placeholder.placeholderDependency = dependency.copy(taskName = item.name)
          }
        }
      item.init(course, parentItem, false)
    } else {
      initTask(project, course, item, info)
    }
  }

  override val studyItemVariants: List<StudyItemVariant>
    get() = listOf(
      StudyItemVariant(
        EduCoreStudyItemBundle.message("task.edu.title"),
        EduCoreActionBundle.message("new.study.item.task.edu.description"),
        Task,
        ::EduTask
      ),
      StudyItemVariant(
        EduCoreStudyItemBundle.message("task.output.title"),
        EduCoreActionBundle.message("new.study.item.task.output.description"),
        Task,
        ::OutputTask
      ),
      StudyItemVariant(
        EduCoreStudyItemBundle.message("task.theory.title"),
        EduCoreActionBundle.message("new.study.item.task.theory.description"),
        Task,
        ::TheoryTask
      ),
      StudyItemVariant(
        EduCoreStudyItemBundle.message("task.choice.title"),
        EduCoreActionBundle.message("new.study.item.task.choice.description"),
        Task
      ) {
        val task = ChoiceTask()
        task.choiceOptions = listOf(
          ChoiceOption("Correct", ChoiceOptionStatus.CORRECT),
          ChoiceOption("Incorrect", ChoiceOptionStatus.INCORRECT)
        )
        task
      },
      StudyItemVariant(
        EduCoreStudyItemBundle.message("task.ide"),
        EduCoreActionBundle.message("new.study.item.task.ide.description"),
        IdeTask,
        ::IdeTask
      )
    )

  private fun initTask(project: Project, course: Course, task: Task, info: NewStudyItemInfo, withSources: Boolean = true) {
    if (!course.isStudy) {
      course.configurator?.courseBuilder?.initNewTask(project, course, task, info, withSources)
    }
  }

  private fun TaskFile.copyForNewTask(taskDir: VirtualFile, newTask: Task): TaskFile {
    val newTaskFile = TaskFile()
    newTaskFile.name = name
    val text = course()?.configurator?.courseBuilder?.getTextForNewTask(this, taskDir, newTask) ?: ""
    newTaskFile.setText(text)
    newTaskFile.isVisible = isVisible
    newTaskFile.answerPlaceholders = answerPlaceholders.map { it.copyForNewTaskFile() }
    return newTaskFile
  }

  private fun AnswerPlaceholder.copyForNewTaskFile(): AnswerPlaceholder {
    val newPlaceholder = AnswerPlaceholder()
    newPlaceholder.placeholderText = placeholderText
    newPlaceholder.offset = offset
    newPlaceholder.length = length
    newPlaceholder.possibleAnswer = possibleAnswer
    newPlaceholder.index = index
    newPlaceholder.hints = ArrayList(hints)
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
