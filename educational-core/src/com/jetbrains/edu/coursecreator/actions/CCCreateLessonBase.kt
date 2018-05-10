package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.*
import javax.swing.Icon

abstract class CCCreateLessonBase<Item : Lesson>(itemName: String, icon: Icon) :
  CCCreateStudyItemActionBase<Item>(itemName, icon) {

  override fun addItem(course: Course, item: Item) {
    val itemContainer = item.container
    itemContainer.addLesson(item)
  }

  override fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem?> {
    return Function { file -> (item as? Lesson)?.container?.getItem(file.name) }
  }

  override fun createItemDir(project: Project, item: Item,
                             parentDirectory: VirtualFile, course: Course): VirtualFile? {
    val configurator = EduConfiguratorManager.forLanguage(course.languageById)
    if (configurator == null) {
      LOG.info("Failed to get configurator for " + course.languageID)
      return null
    }
    return configurator.courseBuilder.createLessonContent(project, item, parentDirectory)
  }

  override fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int {
    return (parentItem as? ItemContainer)?.items?.size ?: 0
  }

  override fun getParentItem(course: Course, directory: VirtualFile): StudyItem? {
    val lesson = EduUtils.getLesson(directory, course)
    return lesson?.container ?: (course.getSection(directory.name) ?: course)
  }

  override fun getThresholdItem(course: Course, sourceDirectory: VirtualFile): StudyItem? = EduUtils.getLesson(sourceDirectory, course)

  override fun isAddedAsLast(sourceDirectory: VirtualFile,
                             project: Project,
                             course: Course): Boolean {
    val section = course.getSection(sourceDirectory.name)
    return section != null || sourceDirectory == EduUtils.getCourseDir(project)
  }

  override fun sortSiblings(course: Course, parentItem: StudyItem?) {
    if (parentItem is ItemContainer) {
      parentItem.sortItems()
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCCreateLessonBase::class.java)
  }
}
