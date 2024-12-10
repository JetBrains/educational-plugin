package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.writeText
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.utils.vfs.createFile
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.visitItems
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import io.mockk.every
import org.junit.Test

class StudyItemIdGeneratorTest : EduTestCase() {

  override fun setUp() {
    super.setUp()
    val mockGenerator = mockService<StudyItemIdGenerator>(project)
    val ids = generateSequence(1, Int::inc).iterator()
    every { mockGenerator.generateNewId() } answers { ids.next() }
  }

  @Test
  fun `generate new ids`() {
    // given
    val course = courseWithFiles {
      section("section1") {
        lesson("lesson1") {
          eduTask("task1") {}
        }
      }
      lesson("lesson2") {
        eduTask("task2") {}
      }
    }

    // when
    StudyItemIdGenerator.getInstance(project).generateIdsIfNeeded(course)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    // then
    course.checkIds {
      section("section1", id = 1) {
        lesson("lesson1", id = 2) {
          eduTask("task1", stepId = 3) {}
        }
      }
      lesson("lesson2", id = 4) {
        eduTask("task2", stepId = 5) {}
      }
    }
  }

  @Test
  fun `do not generate existing ids`() {
    // given
    val course = courseWithFiles {
      section("section1") {
        lesson("lesson1", id = 2) {
          eduTask("task1", stepId = 1) {}
        }
      }
      lesson("lesson2", id = 5) {
        eduTask("task2") {}
      }
    }

    // when
    StudyItemIdGenerator.getInstance(project).generateIdsIfNeeded(course)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    // then
    course.checkIds {
      section("section1", id = 3) {
        lesson("lesson1", id = 2) {
          eduTask("task1", stepId = 1) {}
        }
      }
      lesson("lesson2", id = 5) {
        eduTask("task2", stepId = 4) {}
      }
    }
  }

  @Test
  fun `test take actual changes from remote-info configs`() {
    // given
    val course = courseWithFiles {
      section("section1") {
        lesson("lesson1", id = 2) {
          eduTask("task1") {}
        }
      }
      lesson("lesson2", id = 5) {
        eduTask("task2") {}
      }
    } as EduCourse

    createConfigFiles(project)

    // emulate external changes in `*-remote-info.yaml` files
    val task1Dir = findFile("section1/lesson1/task1")
    runWriteAction {
      task1Dir.createFile(REMOTE_TASK_CONFIG).writeText("id: 1\n")
      findFile("lesson2/$REMOTE_LESSON_CONFIG").writeText("id: 3\n")
    }

    // when
    StudyItemIdGenerator.getInstance(project).generateIdsIfNeeded(course)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    // then
    course.checkIds {
      section("section1", id = 4) {
        lesson("lesson1", id = 2) {
          eduTask("task1", stepId = 1) {}
        }
      }
      lesson("lesson2", id = 3) {
        eduTask("task2", stepId = 5) {}
      }
    }

    checkFileTree {
      file(COURSE_CONFIG)
      dir("section1") {
        file(SECTION_CONFIG)
        file(REMOTE_SECTION_CONFIG, "id: 4\n")
        dir("lesson1") {
          file(LESSON_CONFIG)
          file(REMOTE_LESSON_CONFIG, "id: 2\n")
          dir("task1") {
            file(TASK_CONFIG)
            file(REMOTE_TASK_CONFIG, "id: 1\n")
            file("task.md")
          }
        }
      }
      dir("lesson2") {
        file(LESSON_CONFIG)
        file(REMOTE_LESSON_CONFIG, "id: 3\n")
        dir("task2") {
          file(TASK_CONFIG)
          file(REMOTE_TASK_CONFIG, "id: 5\n")
          file("task.md")
        }
      }
    }
  }

  private fun Course.checkIds(buildExpectedCourse: CourseBuilder.() -> Unit) {
    val expectedCourse = course(buildCourse = buildExpectedCourse)

    val actualIds = collectIds()
    val expectedIds = expectedCourse.collectIds()
    assertEquals(expectedIds, actualIds)
  }

  private fun Course.collectIds(): Map<String, Int> {
    val actualIds = hashMapOf<String, Int>()
    visitItems {
      actualIds[it.name] = it.id
    }
    return actualIds
  }
}
