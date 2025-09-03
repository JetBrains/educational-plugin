package com.jetbrains.edu.csharp.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.csharp.CSharpLanguageSettings
import com.jetbrains.edu.csharp.CSharpProjectSettings
import com.jetbrains.edu.csharp.includeTopLevelDirsInCourseView
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CSharpHyperskillCourseBuilder : EduCourseBuilder<CSharpProjectSettings> {
  override fun getLanguageSettings(): LanguageSettings<CSharpProjectSettings> = CSharpLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CSharpProjectSettings> =
    CSharpHyperskillProjectGenerator(this, course)

  /**
   * Is needed to index top-level files when the Unity project is re-opened as a solution
   */
  override fun refreshProject(project: Project, cause: RefreshCause) {
    super.refreshProject(project, cause)

    if (cause == RefreshCause.STRUCTURE_MODIFIED) {
      includeTopLevelDirsInCourseView(project)
    }
  }
}