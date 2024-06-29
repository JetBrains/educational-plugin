package com.jetbrains.edu.coursecreator.actions.studyItem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.EducationalCoreIcons.CourseView.Lesson
import com.jetbrains.edu.coursecreator.StudyItemType.LESSON_TYPE
import com.jetbrains.edu.coursecreator.presentableTitleName
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CCCreateLesson : CCCreateStudyItemActionBase<Lesson>(LESSON_TYPE, Lesson) {

  override val studyItemVariants: List<StudyItemVariant>
    get() = listOf(
      StudyItemVariant(LESSON_TYPE.presentableTitleName, "", Lesson, ::Lesson),
      StudyItemVariant(EduCoreBundle.message("item.lesson.framework.title"), "", Lesson, ::FrameworkLesson)
    )

  override fun addItem(course: Course, item: Lesson) {
    val itemContainer = item.container
    itemContainer.addLesson(item)
  }

  override fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem?> {
    return Function { file -> (item as? Lesson)?.container?.getItem(file.name) }
  }

  @Throws(IOException::class)
  override fun createItemDir(project: Project, course: Course, item: Lesson, parentDirectory: VirtualFile): VirtualFile {
    return GeneratorUtils.createLesson(project, item, parentDirectory)
  }

  override fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int {
    return (parentItem as? ItemContainer)?.items?.size ?: 0
  }

  override fun getParentItem(project: Project, course: Course, directory: VirtualFile): StudyItem {
    val lesson = directory.getLesson(project)
    return lesson?.container ?: (course.getSection(directory.name) ?: course)
  }

  override fun getThresholdItem(project: Project, course: Course, sourceDirectory: VirtualFile): StudyItem? =
    sourceDirectory.getLesson(project)

  override fun isAddedAsLast(project: Project, course: Course, sourceDirectory: VirtualFile): Boolean {
    val section = sourceDirectory.getSection(project)
    return section != null || sourceDirectory == project.courseDir
  }

  override fun sortSiblings(course: Course, parentItem: StudyItem?) {
    if (parentItem is ItemContainer) {
      parentItem.sortItems()
    }
  }

  override fun initItem(holder: CourseInfoHolder<Course>, parentItem: StudyItem?, item: Lesson, info: NewStudyItemInfo) {
    item.parent = parentItem as? ItemContainer ?: holder.course
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
    @NonNls
    const val ACTION_ID = "Educational.Educator.CreateLesson"
  }
}
