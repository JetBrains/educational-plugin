package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.CourseMode
import com.jetbrains.edu.learning.FileTree
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.createCourseFromJson
import com.jetbrains.edu.learning.stepik.StepikCourseUpdater
import java.util.*

abstract class CourseUpdateTestBase<Settings> : CourseGenerationTestBase<Settings>() {

  protected fun doTest(expectedFileTree: FileTree, testPath: String, modifyCourse: (course: EduCourse) -> Unit = {}) {
    val course = createCourseFromJson("$testPath/course.json", CourseMode.STUDENT)
    val courseFromServer = createCourseFromJson("$testPath/updated_course.json", CourseMode.STUDENT)
    modifyCourse(course)
    doTest(course, courseFromServer, expectedFileTree)
  }

  protected fun doTest(course: EduCourse, courseFromServer: EduCourse, expectedFileTree: FileTree) {
    setTopLevelSection(course)
    createCourseStructure(course)
    setTopLevelSection(courseFromServer)

    StepikCourseUpdater(course, project).doUpdate(courseFromServer)
    assertEquals("Lessons number mismatch. Expected: ${courseFromServer.lessons.size}. Actual: ${course.lessons.size}",
                 courseFromServer.lessons.size, course.lessons.size)

    assertEquals("Sections number mismatch. Expected: ${courseFromServer.sections.size}. Actual: ${course.sections.size}",
                 courseFromServer.sections.size, course.sections.size)

    for ((section, newSection) in course.sections.zip(courseFromServer.sections)) {
      assertTrue("Lesson number mismatch.\n" +
                 "Lesson \"${section.name}\". \n" +
                 "Expected lesson number: ${newSection.lessons.size}. Actual: ${section.lessons.size}",
                 section.lessons.size == newSection.lessons.size)

      checkLessons(section.lessons, newSection.lessons)
    }

    checkLessons(course.lessons, courseFromServer.lessons)


    expectedFileTree.assertEquals(rootDir)
  }

  private fun setTopLevelSection(course: EduCourse) {
    if (course.lessons.isNotEmpty()) {
      // it's a hack.Originally we need to put here and id of remote section for top-level lesson
      course.sectionIds = Collections.singletonList(1)
    }
  }

  private fun checkLessons(lessons: List<Lesson>,
                           lessonsFromServer: List<Lesson>) {
    for ((lesson, newLesson) in lessons.zip(lessonsFromServer)) {
      assertTrue("Tasks number mismatch.\n" +
                          "Lesson \"${lesson.name}\". \n" +
                          "Expected task number: ${newLesson.taskList.size}. Actual: ${lesson.taskList.size}",
                          lesson.taskList.size == newLesson.taskList.size)

      assertTrue("Lesson name mismatch. Expected: ${newLesson.name}. Actual: ${lesson.name}", lesson.name == newLesson.name)
      for ((task, newTask) in lesson.taskList.zip(newLesson.taskList)) {
        assertTrue("Task files number mismatch.\n" +
                            "Lesson \"${lesson.name}\". \n" +
                            "Task \"${task.name}\". \n" +
                            "Expected task files number: ${newTask.taskFiles.size}. Actual: ${task.taskFiles.size}",
                            task.taskFiles.size == newTask.taskFiles.size)

        assertTrue("Task text mismatch.\n" +
                            "Lesson \"${lesson.name}\". \n" +
                            "Task \"${task.name}\". \n" +
                            "Expected:\n \"${newTask.descriptionText}\"\n" +
                            "Actual:\n \"${task.descriptionText}\"",
                            newTask.descriptionText == task.descriptionText)

        assertTrue("Lesson index mismatch.\n Expected: Lesson \"${newLesson.name}\", index: ${newLesson.index}.\n" +
                            " Actual: Lesson \"${lesson.name}\", index: ${lesson.index}", lesson.index == newLesson.index)

      }
    }
  }
}
