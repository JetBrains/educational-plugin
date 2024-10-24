package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.SectionBuilder
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.LessonUpdateTestBase
import org.junit.Test

class HyperskillLessonUpdateTest : LessonUpdateTestBase<HyperskillCourse>() {
  override fun getUpdater(localCourse: HyperskillCourse) = HyperskillCourseUpdaterNew(project, localCourse)

  @Test
  fun `test new lesson in course created`() {
    initiateLocalCourse()

    val newLesson = Lesson().apply {
      name = "lesson2"
      val newEduTask = EduTask("task3").apply {
        id = 3
        taskFiles = linkedMapOf(
          "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
          "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
          "Tests.kt" to TaskFile("test/Tests.kt", "fun test3() {}")
        )
        descriptionFormat = DescriptionFormat.HTML
      }
      addTask(newEduTask)
    }
    val newStages = listOf(
      HyperskillStage(1, "", 1, true),
      HyperskillStage(2, "", 2),
      HyperskillStage(3, "", 3)
    )
    val remoteCourse = toRemoteCourse {
      addLesson(newLesson)
      stages = newStages
    }

    updateCourse(remoteCourse)

    assertEquals("Lesson hasn't been added", 2, getLessons().size)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      dir("lesson2") {
        dir("task3") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test new lesson created in the middle of the course`() {
    localCourse = createBasicHyperskillCourse {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1)
      }
      lesson("lesson2", id = 2) {
        eduTask("task2", stepId = 2)
      }
    }

    val remoteCourse = toRemoteCourse { }
    CourseBuilder(remoteCourse).lesson("lesson3", id = 3, index = 2) {
      eduTask("task3", stepId = 3, taskDescriptionFormat = DescriptionFormat.HTML)
    }
    remoteCourse.apply {
      lessons[1].index = 3
      sortItems()
    }

    updateCourse(remoteCourse)

    val lessons = localCourse.lessons
    assertEquals("Lesson hasn't been added", 3, lessons.size)
    checkIndices(lessons)
    lessons[0].let { lesson ->
      assertEquals(1, lesson.id)
      assertEquals(1, lesson.index)
      assertEquals("lesson1", lesson.name)
    }
    lessons[1].let { lesson ->
      assertEquals(3, lesson.id)
      assertEquals(2, lesson.index)
      assertEquals("lesson3", lesson.name)
    }
    lessons[2].let { lesson ->
      assertEquals(2, lesson.id)
      assertEquals(3, lesson.index)
      assertEquals("lesson2", lesson.name)
    }

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("task.html")
        }
      }
      dir("lesson3") {
        dir("task3") {
          file("task.html")
        }
      }
      dir("lesson2") {
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test new lesson in section created`() {
    localCourse = createBasicHyperskillCourse {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1, taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests.kt", "fun test1() {}")
          }
          eduTask("task2", stepId = 2, taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests.kt", "fun test2() {}")
          }
        }
      }
    }

    val remoteCourse = toRemoteCourse {
      stages = stages + HyperskillStage(3, "", 3)
    }
    SectionBuilder(remoteCourse, remoteCourse.sections[0]).lesson("lesson2", id = 2, index = 2) {
      eduTask("task3", stepId = 3, taskDescriptionFormat = DescriptionFormat.HTML) {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("src/Baz.kt", "fun baz() {}")
        taskFile("test/Tests.kt", "fun test3() {}")
      }
    }

    updateCourse(remoteCourse)

    assertEquals("Lesson hasn't been added", 2, localCourse.sections[0].lessons.size)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
        }
        dir("lesson2") {
          dir("task3") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test new lesson created in the middle of the section`() {
    localCourse = createBasicHyperskillCourse {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1)
        }
        lesson("lesson2", id = 2) {
          eduTask("task2", stepId = 2)
        }
      }
    }

    val remoteCourse = toRemoteCourse { }
    SectionBuilder(remoteCourse, remoteCourse.sections[0]).lesson("lesson3", id = 3, index = 2) {
      eduTask("task3", stepId = 3, taskDescriptionFormat = DescriptionFormat.HTML)
    }
    remoteCourse.sections[0].apply {
      lessons[1].index = 3
      sortItems()
    }

    updateCourse(remoteCourse)

    val lessons = localCourse.sections[0].lessons
    assertEquals("Lesson hasn't been added", 3, lessons.size)
    checkIndices(lessons)
    lessons[0].let { lesson ->
      assertEquals(1, lesson.id)
      assertEquals(1, lesson.index)
      assertEquals("lesson1", lesson.name)
    }
    lessons[1].let { lesson ->
      assertEquals(3, lesson.id)
      assertEquals(2, lesson.index)
      assertEquals("lesson3", lesson.name)
    }
    lessons[2].let { lesson ->
      assertEquals(2, lesson.id)
      assertEquals(3, lesson.index)
      assertEquals("lesson2", lesson.name)
    }

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            file("task.html")
          }
        }
        dir("lesson3") {
          dir("task3") {
            file("task.html")
          }
        }
        dir("lesson2") {
          dir("task2") {
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test lesson in course has been deleted`() {
    localCourse = createBasicHyperskillCourse {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1, taskDescriptionFormat = DescriptionFormat.HTML)
        eduTask("task2", stepId = 2, taskDescriptionFormat = DescriptionFormat.HTML)
      }
      lesson("lesson2", id = 2) {
        eduTask("task3", stepId = 3, taskDescriptionFormat = DescriptionFormat.HTML)
        eduTask("task4", stepId = 4, taskDescriptionFormat = DescriptionFormat.HTML)
      }
    }
    localCourse.stages = listOf(
      HyperskillStage(1, "", 1, true),
      HyperskillStage(2, "", 2),
      HyperskillStage(3, "", 3),
      HyperskillStage(4, "", 4)
    )

    val remoteCourse = toRemoteCourse {
      removeLesson(lessons[0])
      stages = listOf(
        HyperskillStage(3, "", 3),
        HyperskillStage(4, "", 4)
      )
    }

    updateCourse(remoteCourse)

    assertEquals("Lesson hasn't been deleted", 1, getLessons().size)

    val expectedStructure = fileTree {
      dir("lesson2") {
        dir("task3") {
          file("task.html")
        }
        dir("task4") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test lesson in section has been deleted`() {
    localCourse = createBasicHyperskillCourse {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1, taskDescriptionFormat = DescriptionFormat.HTML)
          eduTask("task2", stepId = 2, taskDescriptionFormat = DescriptionFormat.HTML)
        }
        lesson("lesson2", id = 2) {
          eduTask("task3", stepId = 3, taskDescriptionFormat = DescriptionFormat.HTML)
          eduTask("task4", stepId = 4, taskDescriptionFormat = DescriptionFormat.HTML)
        }
      }
    }
    localCourse.stages = listOf(
      HyperskillStage(1, "", 1, true),
      HyperskillStage(2, "", 2),
      HyperskillStage(3, "", 3),
      HyperskillStage(4, "", 4)
    )

    val remoteCourse = toRemoteCourse {
      val section = sections[0]
      section.removeLesson(section.lessons[0])
      stages = listOf(
        HyperskillStage(3, "", 3),
        HyperskillStage(4, "", 4)
      )
    }

    updateCourse(remoteCourse)

    assertEquals("Lesson hasn't been deleted", 1, getLessons(localCourse.sections[0]).size)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson2") {
          dir("task3") {
            file("task.html")
          }
          dir("task4") {
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test new lesson in section created and existing task has been changed`() {
    localCourse = createBasicHyperskillCourse {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1, taskDescriptionFormat = DescriptionFormat.HTML)
          eduTask("task2", stepId = 2, taskDescriptionFormat = DescriptionFormat.HTML)
        }
      }
    }

    val updatedTaskName = "task1 updated"
    val remoteCourse = toRemoteCourse {
      sections[0].lessons[0].taskList[0].name = updatedTaskName
    }
    SectionBuilder(remoteCourse, remoteCourse.sections[0]).lesson("lesson2", id = 2, index = 2) {
      eduTask("task3", stepId = 3, taskDescriptionFormat = DescriptionFormat.HTML)
    }

    updateCourse(remoteCourse)

    val firstLocalSection = localCourse.sections[0]
    val actualTaskName = firstLocalSection.lessons[0].taskList[0].name
    assertEquals("Task name not updated", updatedTaskName, actualTaskName)
    assertEquals("Lesson hasn't been added", 2, firstLocalSection.lessons.size)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir(updatedTaskName) {
            file("task.html")
          }
          dir("task2") {
            file("task.html")
          }
        }
        dir("lesson2") {
          dir("task3") {
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  override fun initiateLocalCourse() {
    localCourse = createBasicHyperskillCourse()
  }
}