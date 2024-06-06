package com.jetbrains.edu.coursecreator.actions.studyItem

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.EducationalCoreIcons.IdeTask
import com.jetbrains.edu.EducationalCoreIcons.Task
import com.jetbrains.edu.coursecreator.StudyItemType.TASK_TYPE
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CCCreateTask : CCCreateStudyItemActionBase<Task>(TASK_TYPE, Task) {

  override fun addItem(course: Course, item: Task) {
    item.lesson.addTask(item)
  }

  override fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem?> =
    Function { file -> (item as? Task)?.lesson?.getTask(file.name) }

  @Throws(IOException::class)
  override fun createItemDir(project: Project, course: Course, item: Task, parentDirectory: VirtualFile): VirtualFile {
    return GeneratorUtils.createTask(project, item, parentDirectory)
  }

  override fun onStudyItemCreation(project: Project, course: Course, item: StudyItem) {
    super.onStudyItemCreation(project, course, item)
    if (!isUnitTestMode) {
      course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
    }
    NavigationUtils.navigateToTask(project, item as Task)
    saveIntoFLStorage(project, item)
  }

  override fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int =
    (parentItem as? Lesson)?.taskList?.size ?: 0

  override fun getParentItem(
    project: Project,
    course: Course,
    directory: VirtualFile
  ): StudyItem? {
    val task = directory.getTask(project) ?: return directory.getLesson(project)
    return task.lesson
  }

  override fun getThresholdItem(project: Project, course: Course, sourceDirectory: VirtualFile): StudyItem? =
    sourceDirectory.getTask(project)

  override fun isAddedAsLast(project: Project, course: Course, sourceDirectory: VirtualFile): Boolean =
    sourceDirectory.getLesson(project) != null

  override fun sortSiblings(course: Course, parentItem: StudyItem?) {
    if (parentItem is Lesson) {
      parentItem.sortItems()
    }
  }

  override fun initItem(holder: CourseInfoHolder<Course>, parentItem: StudyItem?, item: Task, info: NewStudyItemInfo) {
    require(parentItem is Lesson) {
      "parentItem should be Lesson, found `$parentItem`"
    }
    item.parent = parentItem
    item.addDefaultTaskDescription()

    val course = holder.course
    if (parentItem is FrameworkLesson) {
      val prevTask = parentItem.taskList.getOrNull(info.index - 2)
      val prevTaskDir = prevTask?.getDir(holder.courseDir)
      if (prevTask == null || prevTaskDir == null) {
        initTask(course, item, info)
        return
      }
      FileDocumentManager.getInstance().saveAllDocuments()
      // We can't just copy text from course objects because they can contain outdated text
      // in reason that we don't synchronize them with files system
      // So we need to load actual files text from filesystem
      val newTaskFiles = LinkedHashMap<String, TaskFile>()
      val testDirs = course.testDirs
      val defaultTestFileName = course.configurator?.testFileName ?: ""
      val needCopyTests = CCSettings.getInstance().copyTestsInFrameworkLessons
      for ((path, file) in prevTask.taskFiles) {
        if (needCopyTests || !(testDirs.any { path.startsWith(it) } || path == defaultTestFileName)) {
          newTaskFiles[path] = file.copyForNewTask(prevTaskDir, item)
        }
      }
      item.taskFiles = newTaskFiles

      if (!needCopyTests) {
        initTask(course, item, info, withSources = false)
      }

      // If we insert new task between `task1` and `task2`
      // we should change target of all placeholder dependencies of `task2` from task file of `task1`
      // to the corresponding task file in new task
      parentItem.taskList.getOrNull(info.index - 1)
        ?.placeholderDependencies
        ?.forEach { dependency ->
          if (dependency.resolve(course)?.taskFile?.task == prevTask) {
            val placeholder = dependency.answerPlaceholder
            placeholder.placeholderDependency = dependency.copy(taskName = item.name)
          }
        }
      item.init(parentItem, false)
    } else {
      initTask(course, item, info)
    }
  }

  override val studyItemVariants: List<StudyItemVariant>
    get() = listOf(
      StudyItemVariant(
        EduCoreBundle.message("item.task.edu.title"),
        EduCoreBundle.message("action.new.study.item.task.edu.description"),
        Task,
        ::EduTask
      ),
      StudyItemVariant(
        EduCoreBundle.message("item.task.output.title"),
        EduCoreBundle.message("action.new.study.item.task.output.description"),
        Task,
        ::OutputTask
      ),
      StudyItemVariant(
        EduCoreBundle.message("item.task.theory.title"),
        EduCoreBundle.message("action.new.study.item.task.theory.description"),
        EducationalCoreIcons.TheoryTask,
        ::TheoryTask
      ),
      StudyItemVariant(
        EduCoreBundle.message("item.task.choice.title"),
        EduCoreBundle.message("action.new.study.item.task.choice.description"),
        Task
      ) {
        val task = ChoiceTask()
        task.canCheckLocally = true
        task.choiceOptions = listOf(
          ChoiceOption(EduCoreBundle.message("check.correct"), ChoiceOptionStatus.CORRECT),
          ChoiceOption(EduCoreBundle.message("check.incorrect"), ChoiceOptionStatus.INCORRECT)
        )
        task
      },
      StudyItemVariant(
        EduCoreBundle.message("item.task.ide"),
        EduCoreBundle.message("action.new.study.item.task.ide.description"),
        IdeTask,
        ::IdeTask
      )
    )

  private fun initTask(course: Course, task: Task, info: NewStudyItemInfo, withSources: Boolean = true) {
    if (!course.isStudy) {
      course.configurator?.courseBuilder?.initNewTask(course, task, info, withSources)
    }
  }

  private fun TaskFile.copyForNewTask(taskDir: VirtualFile, newTask: Task): TaskFile {
    val newTaskFile = TaskFile()
    newTaskFile.name = name
    val text = course().configurator?.courseBuilder?.getTextForNewTask(this, taskDir, newTask) ?: ""
    newTaskFile.text = text
    newTaskFile.isVisible = isVisible
    newTaskFile.isEditable = isEditable
    newTaskFile.isPropagatable = isPropagatable
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
    newPlaceholder.initialState = AnswerPlaceholder.MyInitialState(initialState.offset, initialState.length)
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

  private fun saveIntoFLStorage(project: Project, item: Task) {
    val lesson = item.parent
    if (lesson !is FrameworkLesson) return
    val flManager = CCFrameworkLessonManager.getInstance(project)
    flManager.saveCurrentState(item)
    val prevTask = lesson.taskList.getOrNull(item.index - 2)
    if (prevTask != null) {
      flManager.saveCurrentState(prevTask)
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.CreateTask"
  }
}
