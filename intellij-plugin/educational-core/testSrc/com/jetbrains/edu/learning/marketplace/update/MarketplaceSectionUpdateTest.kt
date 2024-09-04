package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.SectionUpdateTestBase
import com.jetbrains.edu.learning.update.SectionUpdater
import org.junit.Test

class MarketplaceSectionUpdateTest : SectionUpdateTestBase<EduCourse>() {
  override fun getUpdater(course: Course): SectionUpdater = MarketplaceSectionUpdater(project, course)

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
  fun `test file structure when new section created in the middle of the course`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse) {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1)
        }
      }
      section("section2", id = 2) {
        lesson("lesson2", id = 2) {
          eduTask("task2", stepId = 2)
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as EduCourse

    val newEduTask = EduTask("task3").apply { id = 3 }
    val newLesson = Lesson().apply {
      id = 3
      name = "lesson3"
      addTask(newEduTask)
      newEduTask.parent = this
    }
    val newSection = Section().apply {
      id = 3
      name = "section3"
      addLesson(newLesson)
      newLesson.parent = this
    }

    val remoteCourse = toRemoteCourse {
      val sections = sections.toMutableList()
      sections.add(1, newSection)
      this.sections.forEach { removeSection(it) }
      sections.forEach { addSection(it) }
      init(false)
    }
    updateSections(remoteCourse)
    assertEquals("Section hasn't been added", 3, localCourse.sections.size)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            file("task.md")
          }
        }
      }
      dir("section3") {
        dir("lesson3") {
          dir("task3") {
            file("task.md")
          }
        }
      }
      dir("section2") {
        dir("lesson2") {
          dir("task2") {
            file("task.md")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  // EDU-6756 Support update in case a new StudyItem appears in the middle of the existing ones
  @Test(expected = AssertionError::class)
  fun `test section indexes when new section created in the middle of the course`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse) {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1)
        }
      }
      section("section2", id = 2) {
        lesson("lesson2", id = 2) {
          eduTask("task2", stepId = 2)
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as EduCourse

    val newEduTask = EduTask("task3").apply { id = 3 }
    val newLesson = Lesson().apply {
      id = 3
      name = "lesson3"
      addTask(newEduTask)
      newEduTask.parent = this
    }
    val newSection = Section().apply {
      id = 3
      name = "section3"
      addLesson(newLesson)
      newLesson.parent = this
    }

    val remoteCourse = toRemoteCourse {
      val sections = sections.toMutableList()
      sections.add(1, newSection)
      this.sections.forEach { removeSection(it) }
      sections.forEach { addSection(it) }
      init(false)
    }
    updateSections(remoteCourse)

    val sections = localCourse.sections
    assertEquals("Section hasn't been added", 3, sections.size)
    assertTrue("Wrong index for the first section", sections[0].name == "section1")
    assertTrue("Wrong index for the second section", sections[1].name == "section3")
    assertTrue("Wrong index for the third section", sections[2].name == "section2")
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
    val updatedSectionName = "updated_section"
    val updatedLessonName = "updated_lesson"
    val updatedTaskName = "updated_task"

    val remoteCourse = toRemoteCourse {
      val section = sections.first()
      section.name = updatedSectionName
      val lesson = section.lessons.first()
      lesson.name = updatedLessonName
      val task = lesson.taskList.first()
      task.name = updatedTaskName
    }

    updateSections(remoteCourse)
    val updatedSection = localCourse.sections.first()
    assertEquals(updatedSectionName, updatedSection.name)
    val updatedLesson = updatedSection.lessons.first()
    assertEquals(updatedLessonName, updatedLesson.name)
    val updatedTask = updatedLesson.taskList.first()
    assertEquals(updatedTaskName, updatedTask.name)

    val expectedStructure = fileTree {
      dir(updatedSectionName) {
        dir(updatedLessonName) {
          dir(updatedTaskName) {
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
  fun `test section lessons and tasks added`() {
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
              "Tests3.kt" to TaskFile("test/Tests3.kt", "fun test3() {}")
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
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse) {
      section("section1") {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1, taskDescription = "Task 1 description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests1.kt", "fun test1() {}")
          }
          eduTask("task2", stepId = 2, taskDescription = "Task 2 description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests2.kt", "fun test2() {}")
          }
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as EduCourse
    localCourse.marketplaceCourseVersion = 1
  }
}
