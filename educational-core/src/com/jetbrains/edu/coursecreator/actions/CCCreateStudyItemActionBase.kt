package com.jetbrains.edu.coursecreator.actions

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
import com.jetbrains.edu.coursecreator.actions.sections.CCWrapWithSection
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.CCItemPositionPanel
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.isFeedbackAsked
import com.jetbrains.edu.learning.statistics.showNotification
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.util.*
import javax.swing.Icon

abstract class CCCreateStudyItemActionBase<Item : StudyItem>(
  protected val itemType: StudyItemType,
  icon: Icon
) : DumbAwareAction(
  itemType.presentableTitleName,
  itemType.createItemMessage,
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
    val notification = Notification("WrapLessons", "Wrap Lessons With Section",
                                    "Lessons can be wrapped with section", NotificationType.INFORMATION)
    notification.addAction(object : DumbAwareAction("Wrap lessons") {
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
      val item = createAndInitItem(project, course, parentItem, info)
      val parentDir = getParentDir(project, course, sourceDirectory)
      if (parentDir == null) {
        LOG.info("Failed to get parent directory")
        return@showCreationUI
      }
      CCUtils.updateHigherElements(parentDir.children, getStudyOrderable(item, course), item.index - 1, 1)
      addItem(course, item)
      sortSiblings(course, parentItem)
      val virtualFile = createItemDir(project, course, item, parentDir)
      YamlFormatSynchronizer.saveItem(item)
      YamlFormatSynchronizer.saveItem(item.parent)
      EduCounterUsageCollector.studyItemCreated(item)

      if (virtualFile != null) {
        ProjectView.getInstance(project).select(virtualFile, virtualFile, true)
      }
      askFeedback(course, project)
      if (LessonType == itemType) {
        suggestWrapLessonsIntoSection(project, course, sourceDirectory)
      }
    }
  }

  protected abstract fun addItem(course: Course, item: Item)
  protected abstract fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem>
  protected abstract fun createItemDir(project: Project, course: Course, item: Item, parentDirectory: VirtualFile): VirtualFile?

  private fun showCreationUI(
    project: Project,
    course: Course,
    sourceDirectory: VirtualFile,
    parentItem: StudyItem?,
    dataContext: DataContext,
    studyItemCreator: (NewStudyItemInfo) -> Unit
  ) {
    val index: Int
    val suggestedName: String
    val additionalPanels = ArrayList<AdditionalPanel>()
    if (isAddedAsLast(project, course, sourceDirectory)) {
      index = ITEM_INDEX.getData(dataContext) ?: getSiblingsSize(course, parentItem)
      suggestedName = SUGGESTED_NAME.getData(dataContext) ?: itemType.presentableName + (index + 1)
    }
    else {
      val thresholdItem = getThresholdItem(project, course, sourceDirectory) ?: return
      val defaultIndex = ITEM_INDEX.getData(dataContext)
      index = defaultIndex ?: thresholdItem.index
      val itemName = itemType.presentableName
      suggestedName = SUGGESTED_NAME.getData(dataContext) ?: itemName + (index + 1)
      // If item index is specified by additional params
      // we don't want to ask user about it
      if (defaultIndex == null) {
        additionalPanels.add(CCItemPositionPanel(thresholdItem.name))
      }
    }
    if (parentItem == null) {
      return
    }
    val parentItemDir = parentItem.getDir(project) ?: return
    val model = NewStudyItemUiModel(parentItem, parentItemDir, itemType, suggestedName, index, studyItemVariants)
    showCreateStudyItemDialog(project, course, model, additionalPanels, studyItemCreator)
  }

  protected abstract val studyItemVariants: List<StudyItemVariant>

  protected open fun showCreateStudyItemDialog(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    additionalPanels: List<AdditionalPanel>,
    studyItemCreator: (NewStudyItemInfo) -> Unit
  ) {
    val configurator = course.configurator ?: return
    configurator.courseBuilder.showNewStudyItemUi(project, course, model, additionalPanels, studyItemCreator)
  }

  protected abstract fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int
  protected abstract fun getParentItem(project: Project, course: Course, directory: VirtualFile): StudyItem?
  protected abstract fun getThresholdItem(project: Project, course: Course, sourceDirectory: VirtualFile): StudyItem?
  protected abstract fun isAddedAsLast(project: Project, course: Course, sourceDirectory: VirtualFile): Boolean
  protected abstract fun sortSiblings(course: Course, parentItem: StudyItem?)

  fun createAndInitItem(project: Project, course: Course, parentItem: StudyItem?, info: NewStudyItemInfo): Item {
    @Suppress("UNCHECKED_CAST")
    val item = info.producer() as Item
    item.name = info.name
    item.index = info.index
    initItem(project, course, parentItem, item, info)
    return item
  }

  protected open fun initItem(project: Project, course: Course, parentItem: StudyItem?, item: Item, info: NewStudyItemInfo) {}

  companion object {
    protected val LOG: Logger = Logger.getInstance(CCCreateStudyItemActionBase::class.java)

    @JvmStatic
    val SUGGESTED_NAME: DataKey<String> = DataKey.create("SUGGESTED_NAME")
    @JvmStatic
    val ITEM_INDEX: DataKey<Int> = DataKey.create("ITEM_INDEX")

    private fun askFeedback(course: Course, project: Project) {
      if (isFeedbackAsked()) {
        return
      }
      var countTasks = 0
      course.visitLessons { lesson -> countTasks += lesson.taskList.size }
      if (countTasks == 5) {
        showNotification(false, course, project)
      }
    }

    private fun isActionApplicable(project: Project, selectedFiles: Array<VirtualFile>): Boolean {
      if (selectedFiles.size != 1) return false
      return CCUtils.isCourseCreator(project)
    }
  }
}
