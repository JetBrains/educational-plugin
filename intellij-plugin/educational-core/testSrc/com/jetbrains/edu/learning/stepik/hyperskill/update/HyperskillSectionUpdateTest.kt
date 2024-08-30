package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.SectionUpdateTestBase
import com.jetbrains.edu.learning.update.SectionUpdater
import org.junit.Test

class HyperskillSectionUpdateTest : SectionUpdateTestBase<HyperskillCourse>() {
  override fun getUpdater(course: Course): SectionUpdater = HyperskillSectionUpdater(project, course)

  @Test
  fun `test new section created`() {
    initiateLocalCourse()

    val newEduTask = EduTask("task3").apply {
      id = 3
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests3.kt" to TaskFile("test/Tests3.kt", "fun test3() {}")
      )
      descriptionFormat = DescriptionFormat.HTML
    }
    val newLesson = Lesson().apply {
      id = 2
      name = "lesson2"
      addTask(newEduTask)
      newEduTask.parent = this
    }
    val newSection = Section().apply {
      id = 2
      name = "section2"
      addLesson(newLesson)
      newLesson.parent = this
    }

    val remoteCourse = toRemoteCourse {
      addSection(newSection)
    }
    updateSections(remoteCourse)
    assertEquals("Section hasn't been added", 2, localCourse.sections.size)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests1.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests2.kt")
            }
            file("task.html")
          }
        }
      }
      dir("section2") {
        dir("lesson2") {
          dir("task3") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests3.kt")
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
  fun `test section deleted`() {
    initiateLocalCourse()
    val remoteCourse = toRemoteCourse {
      sections.forEach { removeSection(it) }
    }
    updateSections(remoteCourse)
    assertEquals("Section hasn't been deleted", 0, localCourse.sections.size)

    val expectedStructure = fileTree {
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test section name updated`() {
    initiateLocalCourse()
    val remoteCourse = toRemoteCourse {
      sections[0].name = "updated_section"
    }
    updateSections(remoteCourse)
    val updatedSectionName = localCourse.sections.first().name
    assertEquals("Section name hasn't been updated", "updated_section", updatedSectionName)

    val expectedStructure = fileTree {
      dir("updated_section") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests1.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests2.kt")
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
  fun `test section with lessons and tasks updated`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      sections[0].name = "updated_section"
      val lesson = sections[0].lessons[0]
      lesson.name = "updated_lesson"
      val task = lesson.taskList[0]
      task.name = "updated_task"
    }

    updateSections(remoteCourse)

    val updatedSection = localCourse.sections[0]
    assertEquals("Section hasn't been renamed", "updated_section", updatedSection.name)
    val updatedLesson = updatedSection.lessons[0]
    assertEquals("Lesson hasn't been renamed", "updated_lesson", updatedLesson.name)
    val updatedTask = updatedLesson.taskList[0]
    assertEquals("Task hasn't been renamed", "updated_task", updatedTask.name)

    val expectedStructure = fileTree {
      dir("updated_section") {
        dir("updated_lesson") {
          dir("updated_task") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests1.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests2.kt")
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
  fun `test section lesson and tasks added`() {
    initiateLocalCourse()
    val newLesson = Lesson().apply {
      id = 2
      name = "new_lesson"
      val newTask = EduTask("new_task").apply {
        id = 3
        taskFiles = linkedMapOf(
          "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
          "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
          "Tests1.kt" to TaskFile("test/Tests3.kt", "fun test3() {}")
        )
        descriptionFormat = DescriptionFormat.HTML
      }
      addTask(newTask)
      newTask.parent = this
    }

    val remoteCourse = toRemoteCourse {
      sections[0].addLesson(newLesson)
      newLesson.parent = sections[0]
    }

    updateSections(remoteCourse)
    val updatedSection = localCourse.sections.first()
    assertEquals(2, updatedSection.lessons.size)

    val newAddedLesson = updatedSection.getLesson(2)!!
    assertEquals("new_lesson", newAddedLesson.name)
    assertEquals(1, newAddedLesson.taskList.size)
    assertEquals("new_task", newAddedLesson.getTask(3)!!.name)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests1.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests2.kt")
            }
            file("task.html")
          }
        }
        dir("new_lesson") {
          dir("new_task") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests3.kt")
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
  fun `test sections and lessons swapped and renamed`() {
    initiateLocalCourse()
    val remoteCourse = toRemoteCourse {
      val firstSection = sections[0]
      firstSection.name = "section2"
      val secondSection = Section().apply {
        id = 2
        name = "section1"
        val newLesson = Lesson().apply {
          id = 2
          name = "lesson2"
          val newTask = EduTask("task3").apply {
            id = 3
            taskFiles = linkedMapOf(
              "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
              "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
              "Tests1.kt" to TaskFile("test/Tests3.kt", "fun test3() {}")
            )
            descriptionFormat = DescriptionFormat.HTML
          }
          addTask(newTask)
        }
        addLesson(newLesson)
      }
      sections.forEach { removeSection(it) }
      addSection(secondSection)
      addSection(firstSection)
      init(false)
    }

    updateSections(remoteCourse)
    assertEquals(2, localCourse.sections.size)
    assertEquals("section2", localCourse.sections[0].name)
    assertEquals("section1", localCourse.sections[1].name)
    assertEquals("lesson1", localCourse.sections[0].lessons[0].name)
    assertEquals("lesson2", localCourse.sections[1].lessons[0].name)

    val expectedStructure = fileTree {
      dir("section2") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests1.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests2.kt")
            }
            file("task.html")
          }
        }
      }
      dir("section1") {
        dir("lesson2") {
          dir("task3") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests3.kt")
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

  override fun initiateLocalCourse() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1, taskDescription = "Task 1 description") {
            taskFile("src/Task.kt")
            taskFile("src/Baz.kt")
            taskFile("test/Tests1.kt")
          }
          eduTask("task2", stepId = 2, taskDescription = "Task 2 description") {
            taskFile("src/Task.kt")
            taskFile("src/Baz.kt")
            taskFile("test/Tests2.kt")
          }
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as HyperskillCourse
    localCourse.marketplaceCourseVersion = 1
  }
}
