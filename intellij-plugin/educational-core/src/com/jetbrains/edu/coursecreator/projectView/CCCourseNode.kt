package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.configuration.CourseViewVisibility
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.projectView.CourseNode
import com.jetbrains.edu.learning.projectView.CourseViewUtils.courseViewVisibilityAttribute
import org.jetbrains.annotations.Nls

class CCCourseNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  course: Course
) : CCContentHolderNode, CourseNode(project, value, viewSettings, course) {

  public override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val node = super.modifyChildNode(childNode)
    if (node != null) return node

    val visibility = childNode.courseViewVisibilityAttribute(project, item)
    if (visibility == CourseViewVisibility.INVISIBLE_FOR_ALL) return null

    return when (childNode) {
      is PsiFileNode -> CCStudentInvisibleFileNode(myProject, childNode.value, settings)
      is PsiDirectoryNode -> {
        val psiDirectory = childNode.value
        CCNode(myProject, psiDirectory, settings, null)
      }
      else -> null
    }
  }

  override val additionalInfo: String
    @Nls get() = "(${EduCoreBundle.message("course.creator.course.view.course.creation")})"
}