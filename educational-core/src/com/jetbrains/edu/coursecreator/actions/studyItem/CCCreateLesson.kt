package com.jetbrains.edu.coursecreator.actions.studyItem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.StudyItemType.LESSON_TYPE
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.StudyItemVariant
import com.jetbrains.edu.coursecreator.presentableName
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.learning.messages.EduCoreStudyItemBundle
import icons.EducationalCoreIcons.Lesson

class CCCreateLesson : CCCreateStudyItemActionBase<Lesson>(LESSON_TYPE, Lesson) {

  override val studyItemVariants: List<StudyItemVariant>
    get() = listOf(
      StudyItemVariant(StringUtil.toTitleCase(LESSON_TYPE.presentableName), "",
                                                                                  Lesson, ::Lesson),
      StudyItemVariant(
        StringUtil.toTitleCase(EduCoreStudyItemBundle.message("item.lesson.framework")), "", Lesson, ::FrameworkLesson)
    )

  override fun addItem(course: Course, item: Lesson) {
    val itemContainer = item.container
    itemContainer.addLesson(item)
  }

  override fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem?> {
    return Function { file -> (item as? Lesson)?.container?.getItem(file.name) }
  }

  override fun createItemDir(project: Project, course: Course, item: Lesson, parentDirectory: VirtualFile): VirtualFile? {
    val configurator = course.configurator
    if (configurator == null) {
      LOG.info("Failed to get configurator for " + course.languageID)
      return null
    }
    return configurator.courseBuilder.createLessonContent(project, item, parentDirectory)
  }

  override fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int {
    return (parentItem as? ItemContainer)?.items?.size ?: 0
  }

  override fun getParentItem(project: Project, course: Course, directory: VirtualFile): StudyItem? {
    val lesson = EduUtils.getLesson(project, course, directory)
    return lesson?.container ?: (course.getSection(directory.name) ?: course)
  }

  override fun getThresholdItem(project: Project, course: Course, sourceDirectory: VirtualFile): StudyItem? =
    EduUtils.getLesson(project, course, sourceDirectory)

  override fun isAddedAsLast(project: Project, course: Course, sourceDirectory: VirtualFile): Boolean {
    val section = EduUtils.getSection(project, course, sourceDirectory)
    return section != null || sourceDirectory == project.courseDir
  }

  override fun sortSiblings(course: Course, parentItem: StudyItem?) {
    if (parentItem is ItemContainer) {
      parentItem.sortItems()
    }
  }

  override fun initItem(project: Project, course: Course, parentItem: StudyItem?, item: Lesson, info: NewStudyItemInfo) {
    item.course = course
    if (parentItem is Section) {
      item.section = parentItem
    }
  }

  override fun update(event: AnActionEvent) {
    super.update(event)
    val project = event.getData(CommonDataKeys.PROJECT) ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    val selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
    if (selectedFiles.size != 1) return

    val sourceDirectory = selectedFiles.first()
    if (course.hasSections && getParentItem(project, course, sourceDirectory) is Course) {
      event.presentation.isEnabledAndVisible = false
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCCreateLesson::class.java)
  }
}
