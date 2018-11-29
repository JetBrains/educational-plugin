package com.jetbrains.edu.integration.stepik

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.stepik.CCPushCourse
import com.jetbrains.edu.coursecreator.actions.stepik.CCPushLesson
import com.jetbrains.edu.coursecreator.actions.stepik.CCPushSection
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.StepikConnector

open class StepikIntegrationTest : StepikTestCase() {

  fun `test upload course`() {
    val course = initCourse {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
    }
    checkCourseUploaded(course)
  }

  fun `test upload course with top level lesson`() {
    val localCourse = initCourse {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
    }
    checkTopLevelLessons(localCourse)
  }

  fun `test upload course with framework lesson`() {
    val localCourse = initCourse {
      frameworkLesson("lesson1") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
    }
    checkTopLevelLessons(localCourse)
  }

  fun `test upload course with top level lessons`() {
    val localCourse = initCourse {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
      lesson("lesson2") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
    }

    checkTopLevelLessons(localCourse)
  }

  fun `test post top level lesson`() {
    val localCourse = initCourse {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
      lesson("lesson2") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
    }

    val newLesson = addNewLesson("lesson3", 3, localCourse, localCourse,
                                                                      EduUtils.getCourseDir(project))
    CCPushLesson.doPush(newLesson, project, localCourse)

    checkTopLevelLessons(localCourse)
  }

  fun `test rearrange lessons`() {
    val localCourse = initCourse {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
      lesson("lesson2") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
    }

    val lesson1 = localCourse.getLesson("lesson1")!!
    val lesson2 = localCourse.getLesson("lesson2")!!
    lesson1.index = 2
    lesson2.index = 1
    localCourse.sortItems()
    CCPushLesson.doPush(lesson1, project, localCourse)
    CCPushLesson.doPush(lesson2, project, localCourse)

    checkTopLevelLessons(localCourse)
  }

  fun `test upload course with section`() {
    val localCourse = initCourse {
      section("section1") {
        lesson("lesson1")
        {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }

    checkSections(localCourse)
  }

  fun `test rearrange sections`() {
    val localCourse = initCourse {
      section("section1") {
        lesson("lesson1")
        {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
      section("section2") {
        lesson("lesson1")
        {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }

    val section1 = localCourse.getSection("section1")
    val section2 = localCourse.getSection("section2")
    section1!!.index = 2
    section2!!.index = 1
    localCourse.sortItems()
    CCPushSection.doPush(project, section1, localCourse)
    CCPushSection.doPush(project, section2, localCourse)

    checkSections(localCourse)
  }

  fun `test post lesson into section`() {
    val localCourse = initCourse {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
        lesson("lesson2") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }

    val section = localCourse.getSection("section1")
    val newLesson = addNewLesson("lesson3", 3, localCourse, section!!, EduUtils.getCourseDir(project))
    CCPushLesson.doPush(newLesson, project, StudyTaskManager.getInstance(project).course)

    checkSections(localCourse)
  }

  fun `test rearrange lessons inside section`() {
    val localCourse = initCourse {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
        lesson("lesson2") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }

    val section = localCourse.getSection("section1")
    val newLesson = addNewLesson("lesson3", 3, localCourse, section!!, EduUtils.getCourseDir(project))

    CCPushLesson.doPush(newLesson, project, StudyTaskManager.getInstance(project).course)

    checkSections(localCourse)
  }

  fun `test post new section`() {
    val localCourse = initCourse {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }

    val newSection = addNewSection("section2", 2, localCourse, EduUtils.getCourseDir(project))
    CCPushSection.doPush(project, newSection, localCourse)

    checkSections(localCourse)
  }

  fun `test top-level lessons wrapped into section`() {
    val localCourse = initCourse {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
    }

    CCUtils.wrapIntoSection(project, localCourse, localCourse.lessons, "section1")

    CCPushCourse.doPush(project, localCourse)

    checkSections(localCourse)
  }

  fun `test file texts in task`() {
    val localCourse = courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt")
          testFile("Tests.kt")
          additionalFile("additional_file.txt")
        }
      }
    }

    val taskText = "// task text"
    val testText = "// test text"
    val additionalText = "// additional text"

    setText("lesson1/task1/Task.kt", taskText)
    setText("lesson1/task1/Tests.kt", testText)
    setText("lesson1/task1/additional_file.txt", additionalText)

    CCPushCourse.doPush(project, localCourse)

    val courseFromStepik = getCourseFromStepik(StudyTaskManager.getInstance(project).course!!.id)
    val section = StepikConnector.getSection(courseFromStepik.sectionIds[0])
    val unitIds = section.units.map { unit -> unit.toString() }
    val lessonsFromUnits = StepikConnector.getLessonsFromUnits(courseFromStepik, unitIds.toTypedArray(), false)

    val taskFromStepik = lessonsFromUnits[0].getTask("task1") ?: error("Can't find `task1`")
    assertEquals(taskText, taskFromStepik.getTaskFile("Task.kt")?.getText())
    assertEquals(testText, taskFromStepik.testsText["Tests.kt"])
    assertEquals(additionalText, taskFromStepik.additionalFiles["additional_file.txt"]?.getText())
  }

  private fun setText(path: String, text: String) {
    val file = findFile(path)
    runWriteAction { VfsUtil.saveText(file, text) }
  }

  private fun checkSections(localCourse: Course) {
    val courseFromStepik = getCourseFromStepik(localCourse.id)

    StepikConnector.fillItems(courseFromStepik)
    assertEquals("Sections number mismatch", localCourse.sections.size, courseFromStepik.sections.size)
    localCourse.sections.forEachIndexed { index, section ->
      val sectionFromStepik = courseFromStepik.sections[index]
      assertEquals("Sections name mismatch", section.name, sectionFromStepik.name)
      assertEquals("Lessons number mismatch", section.lessons.size, sectionFromStepik.lessons.size)
      section.lessons.forEachIndexed { lessonIndex, lesson ->
        assertEquals("Lessons name mismatch", lesson.name, sectionFromStepik.lessons[lessonIndex].name)
      }
    }
  }

  private fun checkTopLevelLessons(localCourse: EduCourse) {
    val courseFromStepik = getCourseFromStepik(localCourse.id)

    assertEquals("Course with top-level lessons should have only one section", 1, localCourse.sectionIds.size)

    assertEquals("Top-level lessons section id mismatch", localCourse.sectionIds[0], courseFromStepik.sectionIds[0])
    val section = StepikConnector.getSection(courseFromStepik.sectionIds[0])
    assertEquals("Section name mismatch", localCourse.name, section.name)

    val unitIds = section.units.map { unit -> unit.toString() }
    val lessonsFromUnits = StepikConnector.getLessonsFromUnits(courseFromStepik, unitIds.toTypedArray(), false)

    assertEquals("Lessons number mismatch", localCourse.lessons.size, lessonsFromUnits.size)
    localCourse.lessons.forEachIndexed { index, lesson ->
      assertEquals("Lessons name mismatch", lesson.name, lessonsFromUnits[index].name)
    }
  }

  private fun initCourse(builder: CourseBuilder.() -> Unit): EduCourse {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, buildCourse = builder)
    CCPushCourse.doPush(project, course)
    return StudyTaskManager.getInstance(project).course as EduCourse
  }

  private fun getCourseFromStepik(courseId: Int): EduCourse =
    StepikConnector.getCourseInfo(user, courseId, true) ?: error("Uploaded courses not found among courses available to instructor")
}

internal fun addNewLesson(name: String,
                          index: Int,
                          courseToInit: Course,
                          parent: ItemContainer,
                          virtualFile: VirtualFile): Lesson {
  val course = course {
    lesson(name) {
      eduTask {
        taskFile("fizz.kt")
      }
    }
  }

  val newLesson = course.getLesson(name)!!
  newLesson.index = index
  newLesson.init(courseToInit, parent, false)
  parent.addLesson(newLesson)
  if (parent is Course) {
    GeneratorUtils.createLesson(newLesson, virtualFile)
  }
  else {
    val sectionDir = virtualFile.findChild(parent.name)
    GeneratorUtils.createLesson(newLesson, sectionDir!!)
  }

  return newLesson
}

internal fun addNewSection(name: String,
                           index: Int,
                           courseToInit: Course,
                           virtualFile: VirtualFile): Section {
  val course = course {
    section(name) {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
    }
  }

  val newSection = course.getSection(name)!!
  newSection.index = index
  GeneratorUtils.createSection(newSection, virtualFile)

  courseToInit.addSection(newSection)
  courseToInit.init(null, null, false)
  return newSection
}
