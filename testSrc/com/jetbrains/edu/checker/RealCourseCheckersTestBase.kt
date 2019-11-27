package com.jetbrains.edu.checker

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikTestUtils
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader

/**
 * Base class to test how [TaskChecker]s for dedicated language work with real courses.
 * The main purpose of such tests to ensure that implementation of the corresponding checkers and course structure are consistent.
 *
 * What such test does:
 * * loads course with [courseId] from Stepik
 * * replaces all course placeholders with educator answers
 * * launches [com.jetbrains.edu.learning.actions.CheckAction] for each task
 */
abstract class RealCourseCheckersTestBase<Settings>(private val courseId: Int) : CheckersTestBase<Settings>() {

  override fun createCourse(): Course {
    val mockStepikConnector = StepikConnector.getInstance() as MockStepikConnector
    mockStepikConnector.setBaseUrl(StepikNames.STEPIK_URL, testRootDisposable)
    StepikTestUtils.login(testRootDisposable)
    val course = mockStepikConnector.getCourseInfo(courseId) as EduCourse
    println("'${course.name}' ($courseId) course loading started")
    StepikCourseLoader.loadCourseStructure(course)
    println("'${course.name}' ($courseId) course loading finished")
    course.courseMode = CCUtils.COURSE_MODE
    return course
  }

  open fun `test course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        is TheoryTask, is IdeTask -> ""
        else -> null
      }
    }
    doTest()
  }
}
