package com.jetbrains.edu.coursecreator.actions.studyItem

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.presentableTitleName
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import icons.EducationalCoreIcons
import java.io.IOException

class CCCreateSection : CCCreateStudyItemActionBase<Section>(StudyItemType.SECTION_TYPE, EducationalCoreIcons.Section) {
  override fun addItem(course: Course, item: Section) {
    course.addSection(item)
  }

  override fun getStudyOrderable(item: StudyItem, course: Course): Function<VirtualFile, out StudyItem?> {
    return Function<VirtualFile, StudyItem> { file: VirtualFile -> course.getItem(file.name) }
  }

  @Throws(IOException::class)
  override fun createItemDir(project: Project, course: Course, item: Section, parentDirectory: VirtualFile): VirtualFile? {
    return GeneratorUtils.createSection(item, parentDirectory)
  }

  override fun getSiblingsSize(course: Course, parentItem: StudyItem?): Int = course.items.size

  override fun getParentItem(project: Project, course: Course, directory: VirtualFile): StudyItem? = course

  override fun getThresholdItem(project: Project, course: Course, sourceDirectory: VirtualFile): StudyItem? {
    return course.getItem(sourceDirectory.name)
  }

  override fun isAddedAsLast(project: Project, course: Course, sourceDirectory: VirtualFile): Boolean {
    return sourceDirectory == project.courseDir
  }

  override fun sortSiblings(course: Course, parentItem: StudyItem?) {
    course.sortItems()
  }

  override fun initItem(project: Project, course: Course, parentItem: StudyItem?, item: Section, info: NewStudyItemInfo) {
    item.course = course
  }

  override val studyItemVariants: List<StudyItemVariant>
    get() = listOf(
      StudyItemVariant(StudyItemType.SECTION_TYPE.presentableTitleName, "", EducationalCoreIcons.Section) { Section() }
    )
}
