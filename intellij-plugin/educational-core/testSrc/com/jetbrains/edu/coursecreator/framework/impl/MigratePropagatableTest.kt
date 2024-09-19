package com.jetbrains.edu.coursecreator.framework.impl

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.yaml.YamlMigrator
import com.jetbrains.edu.learning.yaml.YamlMigrator.Companion.YAML_MIGRATED_PROPAGATABLE
import org.junit.Test

class MigratePropagatableTest : EduTestCase() {
  override fun tearDown() {
    try {
      PropertiesComponent.getInstance(project).unsetValue(YAML_MIGRATED_PROPAGATABLE)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test propagatable property sets to false during migration to test files`() {
    val course = courseWithFiles(
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

    assertFalse(yamlMigrationFlag)

    YamlMigrator.getInstance(project, 1).migrateModel(course)

    assertTrue(yamlMigrationFlag)

    assertEquals(true, task.taskFiles["src/Task.kt"]?.isPropagatable)
    assertEquals(true, task.taskFiles["src/Baz.kt"]?.isPropagatable)
    assertEquals(false, task.taskFiles["test/Tests.kt"]?.isPropagatable)
  }

  @Test
  fun `test propagatable property sets to false during migration to invisible or non-editable files`() {
    val course = courseWithFiles(
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

    assertFalse(yamlMigrationFlag)

    YamlMigrator.getInstance(project, 1).migrateModel(course)

    assertTrue(yamlMigrationFlag)

    assertEquals(false, task.taskFiles["src/Task.kt"]?.isPropagatable)
    assertEquals(false, task.taskFiles["src/Baz.kt"]?.isPropagatable)
  }

  @Test
  fun `test propagatable property does not migrate if already migrated`() {
    val course = courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = FakeGradleBasedLanguage
    ) {
      frameworkLesson("lesson") {
        eduTask("task1") {
          taskFile("src/Task.kt", "fun foo() {}", propagatable = true, visible = false)
          taskFile("src/Baz.kt", "fun baz() {}", propagatable = true, editable = false)
          taskFile("test/Tests.kt", "fun tests() {}", propagatable = true)
        }
      }
    }
    val lesson = course.lessons.first()
    val task = lesson.taskList.first()

    yamlMigrationFlag = true

    YamlMigrator.getInstance(project, 1).migrateModel(course)

    assertTrue(yamlMigrationFlag)

    assertEquals(true, task.taskFiles["src/Task.kt"]?.isPropagatable)
    assertEquals(true, task.taskFiles["src/Baz.kt"]?.isPropagatable)
    assertEquals(true, task.taskFiles["test/Tests.kt"]?.isPropagatable)
  }

  @Test
  fun `test propagatable property does not migrate if already has non-propagatable file`() {
    val course = courseWithFiles(
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

    assertFalse(yamlMigrationFlag)

    YamlMigrator.getInstance(project, 1).migrateModel(course)

    assertTrue(yamlMigrationFlag)

    assertEquals(true, task.taskFiles["src/Task.kt"]?.isPropagatable)
    assertEquals(false, task.taskFiles["src/Baz.kt"]?.isPropagatable)
    assertEquals(true, task.taskFiles["test/Tests.kt"]?.isPropagatable)
  }

  private var yamlMigrationFlag: Boolean
    get() = PropertiesComponent.getInstance(project).getBoolean(YAML_MIGRATED_PROPAGATABLE)
    set(value) {
      PropertiesComponent.getInstance(project).setValue(YAML_MIGRATED_PROPAGATABLE, value)
    }
}