package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.python.newProject.PyNewProjectSettings

/**
 * This class is needed as a hack to override behavior of base configurator during Hyperskill course creation
 *
 */
class PyHyperskillBaseConfigurator : PyConfigurator() {
  override fun getMockFileName(text: String): String = MAIN_PY

  override val courseBuilder: EduCourseBuilder<PyNewProjectSettings>
    get() = object : PyCourseBuilder() {
      override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings> {
        return GeneratorWithoutAdditionalFiles(this, course)
      }
    }

  private class GeneratorWithoutAdditionalFiles(builder: PyCourseBuilder, course: Course) : PyCourseProjectGenerator(builder, course) {
    override fun createAdditionalFiles(project: Project, baseDir: VirtualFile, isNewCourse: Boolean) {
      // do nothing
    }
  }
}