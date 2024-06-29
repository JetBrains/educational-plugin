package com.jetbrains.edu.coursecreator.actions.studyItem

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.EducationalCoreIcons.CourseView.Section
import com.jetbrains.edu.coursecreator.StudyItemType.SECTION_TYPE
import com.jetbrains.edu.coursecreator.presentableTitleName
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CCCreateSection : CCCreateStudyItemActionBase<Section>(SECTION_TYPE, Section) {
  override fun addItem(course: Course, item: Section) {
    course.addSection(item)
  }

  override fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem?> {
    return Function<VirtualFile, StudyItem> { file: VirtualFile -> course.getItem(file.name) }
  }

  @Throws(IOException::class)
  override fun createItemDir(project: Project, course: Course, item: Section, parentDirectory: VirtualFile): VirtualFile {
    return GeneratorUtils.createSection(project, item, parentDirectory)
  }

  override fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int = course.items.size

  override fun getParentItem(project: Project, course: Course, directory: VirtualFile): StudyItem = course

  override fun getThresholdItem(project: Project, course: Course, sourceDirectory: VirtualFile): StudyItem? {
    // Threshold item should be located only at the top level
    if (sourceDirectory.parent != project.courseDir) return null
    return course.getItem(sourceDirectory.name)
  }

  override fun isAddedAsLast(project: Project, course: Course, sourceDirectory: VirtualFile): Boolean {
    return sourceDirectory == project.courseDir
  }

  override fun sortSiblings(course: Course, parentItem: StudyItem?) {
    course.sortItems()
  }

  override fun initItem(holder: CourseInfoHolder<Course>, parentItem: StudyItem?, item: Section, info: NewStudyItemInfo) {
    item.parent = holder.course
  }

  override val studyItemVariants: List<StudyItemVariant>
    get() = listOf(
      StudyItemVariant(SECTION_TYPE.presentableTitleName, "", Section) { Section() }
    )

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.CreateSection"
  }
}
