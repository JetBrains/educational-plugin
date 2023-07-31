package com.jetbrains.edu.coursecreator.actions.studyItem

import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.*
import com.jetbrains.edu.coursecreator.StudyItemType.LESSON_TYPE
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.studyItemType
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.isFeedbackAsked
import com.jetbrains.edu.learning.statistics.showPostFeedbackNotification
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.IOException
import javax.swing.Icon

abstract class CCCreateStudyItemActionBase<Item : StudyItem>(
  protected val itemType: StudyItemType,
  icon: Icon
) : DumbAwareAction(
  { itemType.presentableTitleName },
  { itemType.createItemMessage },
  icon
) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
    if (!isActionApplicable(project, selectedFiles)) return
    val course = StudyTaskManager.getInstance(project).course ?: return
    createItem(project, selectedFiles[0], course, e.dataContext)
  }

  private fun suggestWrapLessonsIntoSection(project: Project, course: Course, sourceDirectory: VirtualFile) {
    val parentItem = getParentItem(project, course, sourceDirectory)
    if (parentItem != course) {
      return
    }
    val lessonsToWrap = course.lessons
    if (lessonsToWrap.size != 20) {
      return
    }
    val notification = Notification("JetBrains Academy", EduCoreBundle.message("notification.title.wrap.lessons.with.section"),
                                    EduCoreBundle.message("notification.content.wrap.lessons.with.section"), NotificationType.INFORMATION)
    notification.addAction(object : DumbAwareAction(EduCoreBundle.lazyMessage("action.wrap.lessons.title")) {
      override fun actionPerformed(e: AnActionEvent) {
        CCWrapWithSection.wrapLessonsIntoSection(project, course, lessonsToWrap)
        notification.expire()
      }
    })
    notification.notify(project)
  }

  override fun update(event: AnActionEvent) {
    val presentation = event.presentation
    presentation.isEnabledAndVisible = false
    val project = event.getData(CommonDataKeys.PROJECT) ?: return
    val selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
    if (!isActionApplicable(project, selectedFiles)) return

    val course = StudyTaskManager.getInstance(project).course ?: return

    val sourceDirectory = selectedFiles[0]
    if (!isAddedAsLast(project, course, sourceDirectory) && getThresholdItem(project, course, sourceDirectory) == null) return
    if (CommonDataKeys.PSI_FILE.getData(event.dataContext) != null) return
    presentation.isEnabledAndVisible = true
  }

  private fun getParentDir(project: Project, course: Course, directory: VirtualFile): VirtualFile? {
    return if (isAddedAsLast(project, course, directory)) directory else directory.parent
  }

  private fun createItem(
    project: Project,
    sourceDirectory: VirtualFile,
    course: Course,
    dataContext: DataContext
  ) {
    val parentItem = getParentItem(project, course, sourceDirectory)
    showCreationUI(project, course, sourceDirectory, parentItem, dataContext) { info ->
      val holder = CourseInfoHolder.fromCourse(course, project.courseDir)
      val item = createAndInitItem(holder, parentItem, info)
      val parentDir = getParentDir(project, course, sourceDirectory)
      if (parentDir == null) {
        LOG.info("Failed to get parent directory")
        return@showCreationUI
      }
      CCUtils.updateHigherElements(parentDir.children, getStudyOrderable(item, course), item.index - 1, 1)
      addItem(course, item)
      sortSiblings(course, parentItem)

      val itemDir = try {
        val dir = createItemDir(project, course, item, parentDir)
        onStudyItemCreation(project, course, item)
        dir
      }
      catch (e: IOException) {
        LOG.error("Failed to create ${item.studyItemType.presentableName}", e)
        null
      }

      YamlFormatSynchronizer.saveItem(item)

      val updateParentConfig = UPDATE_PARENT_CONFIG.getData(dataContext) ?: true
      if (updateParentConfig) {
        YamlFormatSynchronizer.saveItem(item.parent)
      }
      EduCounterUsageCollector.studyItemCreated(item)

      if (itemDir != null) {
        ProjectView.getInstance(project).select(itemDir, itemDir, true)
      }
      askFeedback(course, project)
      if (LESSON_TYPE == itemType) {
        suggestWrapLessonsIntoSection(project, course, sourceDirectory)
      }
    }
  }

  protected abstract fun addItem(course: Course, item: Item)
  protected abstract fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem?>

  @Throws(IOException::class)
  protected abstract fun createItemDir(project: Project, course: Course, item: Item, parentDirectory: VirtualFile): VirtualFile?

  protected open fun onStudyItemCreation(project: Project, course: Course, item: StudyItem) {
    val configurator = course.configurator
    if (configurator == null) {
      LOG.info("Failed to get configurator for " + course.languageId)
      return
    }

    configurator.courseBuilder.onStudyItemCreation(project, item)
  }

  private fun showCreationUI(
    project: Project,
    course: Course,
    sourceDirectory: VirtualFile,
    parentItem: StudyItem?,
    dataContext: DataContext,
    studyItemCreator: (NewStudyItemInfo) -> Unit
  ) {
    parentItem ?: return

    val addedAsLast = isAddedAsLast(project, course, sourceDirectory)

    val index = if (addedAsLast) {
      ITEM_INDEX.getData(dataContext) ?: getSiblingsSize(course, parentItem)
    }
    else {
      val thresholdItem = getThresholdItem(project, course, sourceDirectory) ?: return
      ITEM_INDEX.getData(dataContext) ?: thresholdItem.index
    }
    val suggestedName = SUGGESTED_NAME.getData(dataContext) ?: suggestName(
      parentItem,
      itemType.presentableName,
      if (addedAsLast) Int.MAX_VALUE else index
    )

    val parentItemDir = parentItem.getDir(project.courseDir) ?: return
    val model = NewStudyItemUiModel(parentItem, parentItemDir, itemType, suggestedName, index, studyItemVariants)
    showCreateStudyItemDialog(project, course, model, studyItemCreator)
  }

  /**
   * Suggests a name of the form `"$presentableName$index"` that is not already contained in parentItem.
   *
   * The name is suggested to be "good enough" for a new element at the index [insertionIndex].
   * To achieve that, the index is taken to be greater than all indexes from the left of [insertionIndex], and also have
   * the minimal possible value that does not clash with all existing names.
   *
   * For example, if parentItem contains `["abc", "task1", "task5", "task2", "xyz", "task4", "task6"]`, then
   * ```
   * suggestName(parentItem, "task", 0) == "task3"
   * suggestName(parentItem, "task", 1) == "task3"
   * suggestName(parentItem, "task", 2) == "task3"
   * suggestName(parentItem, "task", 3) == "task3"
   * suggestName(parentItem, "task", 4) == "task3"
   * suggestName(parentItem, "task", 5) == "task3"
   * suggestName(parentItem, "task", 6) == "task7"
   * suggestName(parentItem, "task", 7) == "task7"
   * ```
   *
   * if parentItem contains `["abc", "task1", "task5", "task2", "task3", "xyz", "task4", "task6"]`, then
   * ```
   * suggestName(parentItem, "task", /*any value*/) == "task7"
   * ```
   */
  private fun suggestName(parentItem: StudyItem, presentableName: String, insertionIndex: Int): String {
    // parentItem must be ItemContainer, but we add this check to preserve the legacy behaviour
    if (parentItem !is ItemContainer) return "$presentableName${insertionIndex + 1}"

    val prefixLength = presentableName.length
    val items = parentItem.items

    val fixedInsertionIndex = when {
      insertionIndex < 0 -> 0
      insertionIndex > items.size -> items.size
      else -> insertionIndex
    }

    val itemsBefore = items.subList(0, fixedInsertionIndex)
    val itemsAfter = items.subList(fixedInsertionIndex, items.size)

    // Get the list of existing indexes.
    // For example, ["task1", "task4", "dir", "task2"] is converted to [1, 4, 2]
    fun studyItem2index(item: StudyItem): Int? {
      val name = item.name
      if (!name.startsWith(presentableName)) return null
      val extractedIndex = name.substring(prefixLength).toIntOrNull() ?: return null
      return if (extractedIndex <= 0) null else extractedIndex
    }

    val startIndex = (itemsBefore.mapNotNull { studyItem2index(it) }.maxOrNull() ?: 0) + 1

    val nextIndexes = itemsAfter.mapNotNull { studyItem2index(it) }.sorted()

    var suggestedIndex = startIndex
    for (forbiddenIndex in nextIndexes) {
      if (suggestedIndex != forbiddenIndex) {
        return "$presentableName$suggestedIndex"
      }

      suggestedIndex++
    }

    return "$presentableName$suggestedIndex"
  }

  protected abstract val studyItemVariants: List<StudyItemVariant>

  protected open fun showCreateStudyItemDialog(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    studyItemCreator: (NewStudyItemInfo) -> Unit
  ) {
    val configurator = course.configurator ?: return
    configurator.courseBuilder.showNewStudyItemUi(project, course, model, studyItemCreator)
  }

  protected abstract fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int
  protected abstract fun getParentItem(project: Project, course: Course, directory: VirtualFile): StudyItem?
  protected abstract fun getThresholdItem(project: Project, course: Course, sourceDirectory: VirtualFile): StudyItem?
  protected abstract fun isAddedAsLast(project: Project, course: Course, sourceDirectory: VirtualFile): Boolean
  protected abstract fun sortSiblings(course: Course, parentItem: StudyItem?)

  fun createAndInitItem(holder: CourseInfoHolder<Course>, parentItem: StudyItem?, info: NewStudyItemInfo): Item {
    @Suppress("UNCHECKED_CAST")
    val item = info.producer() as Item
    item.name = info.name
    item.index = info.index
    initItem(holder, parentItem, item, info)
    return item
  }

  protected open fun initItem(holder: CourseInfoHolder<Course>, parentItem: StudyItem?, item: Item, info: NewStudyItemInfo) {}

  companion object {
    protected val LOG: Logger = Logger.getInstance(CCCreateStudyItemActionBase::class.java)

    val SUGGESTED_NAME: DataKey<String> = DataKey.create("SUGGESTED_NAME")

    val ITEM_INDEX: DataKey<Int> = DataKey.create("ITEM_INDEX")

    val UPDATE_PARENT_CONFIG: DataKey<Boolean> = DataKey.create("UPDATE_PARENT_CONFIG")

    private fun askFeedback(course: Course, project: Project) {
      if (isFeedbackAsked()) {
        return
      }
      var countTasks = 0
      course.visitLessons { lesson -> countTasks += lesson.taskList.size }
      if (countTasks == 5) {
        showPostFeedbackNotification(false, course, project)
      }
    }

    private fun isActionApplicable(project: Project, selectedFiles: Array<VirtualFile>): Boolean {
      if (selectedFiles.size != 1) return false
      return CCUtils.isCourseCreator(project)
    }
  }
}
