package com.jetbrains.edu.python.learning

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.python.learning.newproject.installVersionSpecifiers
import com.jetbrains.python.PythonLanguage
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PyInstallVersionSpecifiersTest(
  private val course: Course,
  private val suitableVersions: List<String>,
  private val nonsuitableVersions: List<String>
) {

  @Test
  fun `install uses latest non-forbidden Python for special courses`() {
    val specifiers = installVersionSpecifiers(course)
    for (suitableVersion in suitableVersions) {
      Assert.assertTrue("Course ${course.id} should allow Python $suitableVersion", specifiers.isValid(suitableVersion))
    }
    for (nonsuitableVersion in nonsuitableVersions) {
      Assert.assertFalse("Course ${course.id} should not allow Python $nonsuitableVersion", specifiers.isValid(nonsuitableVersion))
    }
  }

  companion object {

    private fun python3Course(courseId: Int, languageVersion: String = EduFormatNames.PYTHON_3_VERSION): EduCourse = EduCourse().apply {
      id = courseId
      languageId = PythonLanguage.INSTANCE.id
      this.languageVersion = languageVersion
    }

    @JvmStatic
    @Parameterized.Parameters(name = "{index}")
    fun data(): Collection<Array<Any>> {
      return listOf(
        arrayOf(python3Course(28816), listOf("3.11", "3.11.1", "3.12.5", "3.13", "3.13.1"), listOf("2.7", "3.14", "3.14.2", "3.15")),
        arrayOf(python3Course(42), listOf("3.11", "3.11.1", "3.12.5", "3.13", "3.13.1", "3.14", "3.14.2", "3.15"), listOf("2.7")),
        arrayOf(python3Course(42, "3.10"), listOf("3.11", "3.11.1", "3.12.5", "3.13", "3.13.1", "3.14", "3.14.2", "3.15"), listOf("2.7", "3.9")),
        arrayOf(python3Course(28816, "3.12"), listOf("3.12.5", "3.13", "3.13.1"), listOf("2.7", "3.9", "3.11", "3.11.1", "3.14", "3.14.2", "3.15"))
      )
    }
  }
}