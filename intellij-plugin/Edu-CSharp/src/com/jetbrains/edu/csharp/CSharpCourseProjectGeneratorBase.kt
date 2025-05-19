package com.jetbrains.edu.csharp

import com.intellij.util.io.createDirectories
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.rider.services.security.TrustedSolutionStore
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.notExists

abstract class CSharpCourseProjectGeneratorBase(builder: EduCourseBuilder<CSharpProjectSettings>, course: Course) :
  CourseProjectGenerator<CSharpProjectSettings>(builder, course) {
  /**
   * Rider uses a custom .idea dir which is .idea\.idea.<project_name>
   * See [com.jetbrains.edu.csharp.CSharpCourseProjectGeneratorBase.setUpProjectLocation]
   *
   * Actually, when opened as a folder, the project should have the regular .idea dir and when the project is
   * re-opened as a solution, the contents of .idea dir should migrate to .idea\.idea.<project_name>.
   * This migration doesn't happen for some reason in our case, so the artificial directory is set up from the very beginning
   */
  override fun setUpProjectLocation(location: Path): Path {
    val baseIdeaDir = location.resolve(".idea")
    val projectDir = baseIdeaDir.resolve(".idea.${location.name}")

    TrustedSolutionStore.getInstance().assumeTrusted(projectDir)

    if (projectDir.notExists()) {
      projectDir.createDirectories()
    }
    return projectDir
  }
}