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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.CCUtils
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
  StringUtil.toTitleCase(itemType.presentableName),
  "Create New " + StringUtil.toTitleCase(itemType.presentableName),
  icon
) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
    if (!isActionApplicable(project, selectedFiles)) return

    val course = StudyTaskManager.getInstance(project).course ?: return

    val itemFile = createItem(project, selectedFiles[0], course, e.dataContext)
    if (itemFile != null) {
      ProjectView.getInstance(project).select(itemFile, itemFile, true)
    }
    askFeedback(course, project)
    if (StudyItemType.LESSON == itemType) {
      suggestWrapLessonsIntoSection(project, course, selectedFiles[0])
    }
  }

  private fun suggestWrapLessonsIntoSection(project: Project, course: Course, sourceDirectory: VirtualFile) {
    val parentItem = getParentItem(course, sourceDirectory)
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
    if (!isAddedAsLast(sourceDirectory, project, course) && getThresholdItem(course, sourceDirectory) == null) return
    if (CommonDataKeys.PSI_FILE.getData(event.dataContext) != null) return
    presentation.isEnabledAndVisible = true
  }

  protected fun getParentDir(project: Project, course: Course, directory: VirtualFile): VirtualFile? {
    return if (isAddedAsLast(directory, project, course)) directory else directory.parent
  }

  fun createItem(
    project: Project,
    sourceDirectory: VirtualFile,
    course: Course,
    dataContext: DataContext
  ): VirtualFile? {
    val parentItem = getParentItem(course, sourceDirectory)
    val item = getItem(project, course, sourceDirectory, parentItem, dataContext)
    if (item == null) {
      LOG.info("Failed to create study item")
      return null
    }
    val parentDir = getParentDir(project, course, sourceDirectory)
    if (parentDir == null) {
      LOG.info("Failed to get parent directory")
      return null
    }
    CCUtils.updateHigherElements(parentDir.children, getStudyOrderable(item, course), item.index - 1, 1)
    addItem(course, item)
    sortSiblings(course, parentItem)
    val virtualFile = createItemDir(project, item, parentDir, course)
    YamlFormatSynchronizer.saveItem(item)
    YamlFormatSynchronizer.saveItem(item.parent)
    EduCounterUsageCollector.studyItemCreated(item)
    return virtualFile
  }

  protected abstract fun addItem(course: Course, item: Item)
  protected abstract fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem>
  protected abstract fun createItemDir(project: Project, item: Item, parentDirectory: VirtualFile, course: Course): VirtualFile?

  protected fun getItem(
    project: Project,
    course: Course,
    sourceDirectory: VirtualFile,
    parentItem: StudyItem?,
    dataContext: DataContext
  ): Item? {
    val index: Int
    val suggestedName: String
    val additionalPanels = ArrayList<AdditionalPanel>()
    if (isAddedAsLast(sourceDirectory, project, course)) {
      index = ITEM_INDEX.getData(dataContext) ?: getSiblingsSize(course, parentItem) + 1
      suggestedName = SUGGESTED_NAME.getData(dataContext) ?: itemType.presentableName + index
    }
    else {
      val thresholdItem = getThresholdItem(course, sourceDirectory) ?: return null
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
      return null
    }
    val parentItemDir = parentItem.getDir(project) ?: return null
    val model = NewStudyItemUiModel(parentItem, parentItemDir, itemType, suggestedName, index)
    val info = showCreateStudyItemDialog(project, course, model, additionalPanels) ?: return null
    return createAndInitItem(project, course, parentItem, info)
  }

  protected open fun showCreateStudyItemDialog(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    additionalPanels: List<AdditionalPanel>
  ): NewStudyItemInfo? {
    val configurator = course.configurator ?: return null
    return configurator.courseBuilder.showNewStudyItemUi(project, course, model, additionalPanels)
  }

  protected abstract fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int
  protected abstract fun getParentItem(course: Course, directory: VirtualFile): StudyItem?
  protected abstract fun getThresholdItem(course: Course, sourceDirectory: VirtualFile): StudyItem?
  protected abstract fun isAddedAsLast(sourceDirectory: VirtualFile, project: Project, course: Course): Boolean
  protected abstract fun sortSiblings(course: Course, parentItem: StudyItem?)

  abstract fun createAndInitItem(project: Project, course: Course, parentItem: StudyItem?, info: NewStudyItemInfo): Item?

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
