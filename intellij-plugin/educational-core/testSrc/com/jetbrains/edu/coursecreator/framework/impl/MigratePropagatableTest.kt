package com.jetbrains.edu.coursecreator.framework.impl

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.EduProjectActivity.Companion.YAML_MIGRATED_PROPAGATABLE
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.configurators.FakeGradleConfigurator
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.registerConfigurator
import org.junit.Test

class MigratePropagatableTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

  override fun setUp() {
    super.setUp()
    registerConfigurator<FakeGradleConfigurator>(FakeGradleBasedLanguage)
  }

  @Test
  fun `test propagatable property sets to false during migration to test files`() {
    val course = course(
      courseMode = CourseMode.EDUCATOR,
      language = FakeGradleBasedLanguage
    ) {
      frameworkLesson("lesson") {
        eduTask("task1") {
          taskFile("src/Task.kt", "fun foo() {}", propagatable = true)
          taskFile("src/Baz.kt", "fun baz() {}", propagatable = true)
          taskFile("test/Tests.kt", "fun tests() {}", propagatable = true)
        }
      }
    }
    val lesson = course.lessons.first()
    val task = lesson.taskList.first()

    createCourseStructure(course)

    assertTrue(yamlMigrationFlag)

    assertEquals(true, task.taskFiles["src/Task.kt"]?.isPropagatable)
    assertEquals(true, task.taskFiles["src/Baz.kt"]?.isPropagatable)
    assertEquals(false, task.taskFiles["test/Tests.kt"]?.isPropagatable)
  }

  @Test
  fun `test propagatable property sets to false during migration to invisible or non-editable files`() {
    val course = course(
      courseMode = CourseMode.EDUCATOR,
      language = FakeGradleBasedLanguage
    ) {
      frameworkLesson("lesson") {
        eduTask("task1") {
          taskFile("src/Task.kt", "fun foo() {}", propagatable = true, visible = false)
          taskFile("src/Baz.kt", "fun baz() {}", propagatable = true, editable = false)
        }
      }
    }
    val lesson = course.lessons.first()
    val task = lesson.taskList.first()

    createCourseStructure(course)

    assertTrue(yamlMigrationFlag)

    assertEquals(false, task.taskFiles["src/Task.kt"]?.isPropagatable)
    assertEquals(false, task.taskFiles["src/Baz.kt"]?.isPropagatable)
  }

  @Test
  fun `test propagatable property does not migrate if already has non-propagatable file`() {
    val course = course(
      courseMode = CourseMode.EDUCATOR,
      language = FakeGradleBasedLanguage
    ) {
      frameworkLesson("lesson") {
        eduTask("task1") {
          taskFile("src/Task.kt", "fun foo() {}", propagatable = true, visible = false)
          taskFile("src/Baz.kt", "fun baz() {}", propagatable = false, editable = false)
          taskFile("test/Tests.kt", "fun tests() {}", propagatable = true)
        }
      }
    }
    val lesson = course.lessons.first()
    val task = lesson.taskList.first()

    createCourseStructure(course)

    assertTrue(yamlMigrationFlag)

    assertEquals(true, task.taskFiles["src/Task.kt"]?.isPropagatable)
    assertEquals(false, task.taskFiles["src/Baz.kt"]?.isPropagatable)
    assertEquals(true, task.taskFiles["test/Tests.kt"]?.isPropagatable)
  }

  private val yamlMigrationFlag: Boolean
    get() = PropertiesComponent.getInstance(project).getBoolean(YAML_MIGRATED_PROPAGATABLE)
}
