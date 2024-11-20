package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings

/**
 * This class is needed as a hack to override behavior of base configurator during Hyperskill course creation
 *
 */
class PyHyperskillBaseConfigurator : PyConfigurator() {
  override fun getMockFileName(course: Course, text: String): String = MAIN_PY

  override val courseBuilder: EduCourseBuilder<PyProjectSettings>
    get() = PyHyperskillCourseBuilder()

  private class PyHyperskillCourseBuilder : PyCourseBuilder() {
    override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyProjectSettings> {
      return GeneratorWithoutAdditionalFiles(this, course)
    }
  }

  private class GeneratorWithoutAdditionalFiles(builder: PyCourseBuilder, course: Course) : PyCourseProjectGenerator(builder, course) {
    override fun createAdditionalFiles(holder: CourseInfoHolder<Course>) {
      // do nothing
    }
  }
}