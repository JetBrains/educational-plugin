package com.jetbrains.edu.rust

import com.jetbrains.edu.learning.*
import org.rust.lang.RsLanguage

class RsCourseBuilderTest : CourseGenerationTestBase<RsProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<RsProjectSettings> = RsCourseBuilder()
  override val defaultSettings: RsProjectSettings = RsProjectSettings(null)

  fun `test new educator course`() {
    val newCourse = newCourse(RsLanguage)
    createCourseStructure(courseBuilder, newCourse, defaultSettings)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("lib.rs")
          file("main.rs")
        }
        dir("tests") {
          file("tests.rs")
        }
        file("Cargo.toml")
        file("task.html")
      }
    }.assertEquals(rootDir)
  }

  fun `test study course structure`() {
    val course = course(language = RsLanguage) {
      lesson {
        eduTask {
          taskFile("src/main.rs")
          taskFile("tests/tests.rs")
          taskFile("Cargo.toml")
        }
      }
    }
    createCourseStructure(courseBuilder, course, defaultSettings)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("main.rs")
        }
        dir("tests") {
          file("tests.rs")
        }
        file("Cargo.toml")
      }
    }.assertEquals(rootDir)
  }
}
