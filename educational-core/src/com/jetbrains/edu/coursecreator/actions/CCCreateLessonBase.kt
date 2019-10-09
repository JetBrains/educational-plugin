package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import javax.swing.Icon

abstract class CCCreateLessonBase<Item : Lesson>(itemType: StudyItemType, icon: Icon) :
  CCCreateStudyItemActionBase<Item>(itemType, icon) {

  override fun addItem(course: Course, item: Item) {
    val itemContainer = item.container
    itemContainer.addLesson(item)
  }

  override fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem?> {
    return Function { file -> (item as? Lesson)?.container?.getItem(file.name) }
  }

  override fun createItemDir(project: Project, course: Course, item: Item, parentDirectory: VirtualFile): VirtualFile? {
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
    private val LOG: Logger = Logger.getInstance(CCCreateLessonBase::class.java)
  }
}
