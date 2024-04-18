package com.jetbrains.edu.csharp

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rider.projectView.*
import java.nio.file.Path
import kotlin.io.path.pathString

class CSharpCourseProjectGenerator(
  builder: CSharpCourseBuilder,
  course: Course
) : CourseProjectGenerator<CSharpProjectSettings>(builder, course) {
  override suspend fun openNewCourseProject(location: Path, prepareToOpenCallback: suspend (Project, Module) -> Unit): Project? {
    val task = Companion.OpenProjectTask(course, prepareToOpenCallback).run {
      copy(
        beforeInit = {
          it.putUserData(EDU_PROJECT_CREATED, true)
          val description = SolutionDescriptionFactory.virtual(course.name, listOf(location.pathString))
          val strategy = RdOpenSolution(description, false)
          SolutionInitializer.initSolution(it, strategy)
        }
      )
    }
    return ProjectManagerEx.getInstanceEx().openProjectAsync(location, task)
  }
}