package com.jetbrains.edu.slow.integration.stepik

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.stepik.CCPushCourse
import com.jetbrains.edu.coursecreator.actions.stepik.CCPushLesson
import com.jetbrains.edu.coursecreator.actions.stepik.CCPushSection
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader
import com.jetbrains.edu.learning.stepik.api.loadAndFillLessonAdditionalInfo

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

    val newLesson = addNewLesson("lesson3", 3, localCourse, localCourse, project.courseDir)
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
    CCPushCourse.doPush(project, localCourse)

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
    CCPushCourse.doPush(project, localCourse)

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
    val newLesson = addNewLesson("lesson3", 3, localCourse, section!!, project.courseDir)
    CCPushLesson.doPush(newLesson, project, StudyTaskManager.getInstance(project).course as EduCourse)

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
    val newLesson = addNewLesson("lesson3", 3, localCourse, section!!, project.courseDir)

    CCPushLesson.doPush(newLesson, project, StudyTaskManager.getInstance(project).course as EduCourse)

    checkSections(localCourse)
  }

  fun `test custom lesson name`() {
    val localCourse = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
      }
    }
    val newName = "renamed"
    localCourse.lessons[0].customPresentableName = newName

    CCPushCourse.doPush(project, localCourse.asEduCourse())
    val courseFromStepik = getCourseFromStepik(StudyTaskManager.getInstance(project).course!!.id)
    val section = StepikConnector.getInstance().getSection(courseFromStepik.sectionIds[0])!!
    val lesson = StepikCourseLoader.getLessonsFromUnits(courseFromStepik, section.units, false)[0]
    loadAndFillLessonAdditionalInfo(lesson)

    assertEquals(newName, lesson.presentableName)
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

    val newSection = addNewSection("section2", 2, localCourse, project.courseDir)
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
          taskFile("src/Task.kt")
          taskFile("test/Tests.kt")
          taskFile("build.gradle")
        }
      }
    }

    val taskText = "// task text"
    val testText = "// test text"
    val additionalText = "// additional text"

    setText("lesson1/task1/src/Task.kt", taskText)
    setText("lesson1/task1/test/Tests.kt", testText)
    setText("lesson1/task1/build.gradle", additionalText)

    CCPushCourse.doPush(project, localCourse.asEduCourse())

    val courseFromStepik = getCourseFromStepik(StudyTaskManager.getInstance(project).course!!.id)
    val section = StepikConnector.getInstance().getSection(courseFromStepik.sectionIds[0])!!
    val lessonsFromUnits = StepikCourseLoader.getLessonsFromUnits(courseFromStepik, section.units, false)

    val taskFromStepik = lessonsFromUnits[0].getTask("task1") ?: error("Can't find `task1`")
    assertEquals(taskText, taskFromStepik.getTaskFile("src/Task.kt")?.text)
    assertEquals(testText, taskFromStepik.getTaskFile("test/Tests.kt")?.text)
    assertEquals(additionalText, taskFromStepik.getTaskFile("build.gradle")?.text)
  }

  fun `test file text in Quiz task`() {
    val choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)

    val localCourse = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        choiceTask("Quiz", choiceOptions = choiceOptions) {
          taskFile("src/Task.kt")
        }
      }
    }

    val taskText = "// task text"
    setText("lesson1/Quiz/src/Task.kt", taskText)
    CCPushCourse.doPush(project, localCourse.asEduCourse())

    val courseFromStepik = getCourseFromStepik(StudyTaskManager.getInstance(project).course!!.id)
    val section = StepikConnector.getInstance().getSection(courseFromStepik.sectionIds[0])!!
    val lesson = StepikCourseLoader.getLessonsFromUnits(courseFromStepik, section.units, false)[0]
    loadAndFillLessonAdditionalInfo(lesson)

    val taskFromStepik = lesson.getTask("Quiz") ?: error("Can't find `Quiz`")
    assertEquals(taskText, taskFromStepik.getTaskFile("src/Task.kt")?.text)
  }

  fun `test placeholders in Quiz task`() {
    val choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)

    val localCourse = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        choiceTask("Quiz", choiceOptions = choiceOptions) {
          taskFile("src/Task.kt", "fun foo(): String = <p>Foo</p>") {
            placeholder(0)
          }
        }
      }
    }

    findPlaceholder(0, 0, "src/Task.kt", 0).apply {
      placeholderText = "TODO()"
    }

    CCPushCourse.doPush(project, localCourse.asEduCourse())

    val courseFromStepik = getCourseFromStepik(StudyTaskManager.getInstance(project).course!!.id)
    val section = StepikConnector.getInstance().getSection(courseFromStepik.sectionIds[0])!!
    val lesson = StepikCourseLoader.getLessonsFromUnits(courseFromStepik, section.units, false)[0]
    loadAndFillLessonAdditionalInfo(lesson)

    val taskFromStepik = lesson.getTask("Quiz") ?: error("Can't find `Quiz`")
    assertEquals("fun foo(): String = TODO()", taskFromStepik.getTaskFile("src/Task.kt")?.text)
  }

  fun `test course with language version`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {}
    course.language = course.language + " 2"
    val expectedLanguage = course.language
    CCPushCourse.doPush(project, course.asEduCourse())
    val uploadedCourse = StudyTaskManager.getInstance(project).course as EduCourse
    val remoteCourse = getCourseFromStepik(uploadedCourse.id)
    assertEquals(expectedLanguage, remoteCourse.language)
  }

  fun `test course with choice task`() {
    val expectedChoiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.CORRECT)
    val taskName = "Greatest name ever"
    val localCourse = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        choiceTask(taskName, isMultipleChoice = true, choiceOptions = expectedChoiceOptions) {
          taskFile("text.txt")
        }
      }
    }

    val sourceChoiceTask = findTask(0, 0) as ChoiceTask
    val correct = "correct"
    val incorrect = "incorrect"

    sourceChoiceTask.messageCorrect = correct
    sourceChoiceTask.messageIncorrect = incorrect

    val sourceCourse = initCourse(localCourse)

    val courseFromStepik = getCourseFromStepik(sourceCourse.id)
    checkCourseUploaded(sourceCourse)
    StepikCourseLoader.fillItems(courseFromStepik)

    val task = (courseFromStepik.items[0] as Lesson).taskList[0]
    assertInstanceOf(task, ChoiceTask::class.java)

    val choiceTask = task as ChoiceTask
    assertTrue(choiceTask.isMultipleChoice)
    assertEquals(expectedChoiceOptions, choiceTask.choiceOptions.associateBy({ it.text }, { it.status }))
    assertEquals(correct, choiceTask.messageCorrect)
    assertEquals(incorrect, choiceTask.messageIncorrect)

    assertEquals(taskName, choiceTask.name)
  }

  fun `test upload course with hidden solutions`() {
    val course = courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("fizz.kt")
        }
        eduTask("task2") {
          taskFile("fizz.kt")
        }
      }
    }
    course.solutionsHidden = true
    course.getLesson("lesson1")!!.getTask("task1")!!.solutionHidden = true
    course.getLesson("lesson1")!!.getTask("task2")!!.solutionHidden = false
    val sourceCourse = initCourse(course)
    val courseFromStepik = getCourseFromStepik(sourceCourse.id)
    checkCourseUploaded(sourceCourse)
    StepikCourseLoader.fillItems(courseFromStepik)
    assertTrue(courseFromStepik.solutionsHidden)
    assertTrue(courseFromStepik.getLesson("lesson1")!!.getTask("task1")!!.solutionHidden!!)
    assertFalse(courseFromStepik.getLesson("lesson1")!!.getTask("task2")!!.solutionHidden!!)
  }

  private fun setText(path: String, text: String) {
    val file = findFile(path)
    runWriteAction { VfsUtil.saveText(file, text) }
  }

  private fun checkSections(localCourse: Course) {
    val courseFromStepik = getCourseFromStepik(localCourse.id)

    StepikCourseLoader.fillItems(courseFromStepik)
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
    val section = StepikConnector.getInstance().getSection(courseFromStepik.sectionIds[0])!!
    assertEquals("Section name mismatch", localCourse.name, section.name)

    val lessonsFromUnits = StepikCourseLoader.getLessonsFromUnits(courseFromStepik, section.units, false)

    assertEquals("Lessons number mismatch", localCourse.lessons.size, lessonsFromUnits.size)
    localCourse.lessons.forEachIndexed { index, lesson ->
      assertEquals("Lessons name mismatch", lesson.name, lessonsFromUnits[index].name)
    }
  }

  private fun initCourse(builder: CourseBuilder.() -> Unit): EduCourse {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, buildCourse = builder)
    return initCourse(course)
  }

  private fun initCourse(course: Course): EduCourse {
    CCPushCourse.doPush(project, course.asEduCourse())
    return StudyTaskManager.getInstance(project).course as EduCourse
  }

  private fun getCourseFromStepik(courseId: Int): EduCourse =
    StepikConnector.getInstance().getCourseInfo(courseId, true) ?: error(
      "Uploaded course `$courseId` not found among courses available to instructor")
}

internal fun addNewLesson(name: String,
                          index: Int,
                          courseToInit: Course,
                          parent: LessonContainer,
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
  courseToInit.addSection(newSection)
  courseToInit.init(null, null, false)
  GeneratorUtils.createSection(newSection, virtualFile)
  return newSection
}
