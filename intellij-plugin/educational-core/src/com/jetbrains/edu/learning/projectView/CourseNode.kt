package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.configuration.excludeFromArchive
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.projectView.CourseViewUtils.extractNodeFromPsiDirectory
import com.jetbrains.edu.learning.projectView.FrameworkLessonNode.Companion.createFrameworkLessonNode

open class CourseNode(
  project: Project,
  value: PsiDirectory,
  settings: ViewSettings,
  course: Course
) : EduNode<Course>(project, value, settings, course) {

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val directory = childNode.value as? PsiDirectory ?: return null
    val node =  extractNodeFromPsiDirectory(directory, item, myProject, ::createSectionNode, ::createLessonNode, ::createIntermediateDirectoryNode)
    if (node != null) {
      return node
    }
    if (item.configurator?.shouldFileBeVisibleToStudent(directory.virtualFile) == true) {
      return childNode
    }
    return null
  }

  protected open fun createSectionNode(directory: PsiDirectory, section: Section): SectionNode {
    return SectionNode(myProject, settings, section, directory)
  }

  protected open fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode? {
    return if (lesson is FrameworkLesson) {
      createFrameworkLessonNode(myProject, directory, settings, lesson)
    }
    else {
      LessonNode(myProject, directory, settings, lesson)
    }
  }

  protected open fun createIntermediateDirectoryNode(directory: PsiDirectory): IntermediateDirectoryNode {
    return IntermediateDirectoryNode(myProject, directory, settings)
  }

  override val additionalInfo: String?
    get() {
      if (item is HyperskillCourse) {
        return null
      }
      val (tasksSolved, tasksTotal) = ProgressUtil.countProgress(item)
      return " $tasksSolved/$tasksTotal"
    }

  override val item: Course get() = super.item!!
}
