package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.projectView.IntermediateDirectoryNode

class CCIntermediateDirectoryNode(
  project: Project,
  course: Course,
  value: PsiDirectory,
  viewSettings: ViewSettings,
) : CCContentHolderNode, IntermediateDirectoryNode(project, value, viewSettings, course)