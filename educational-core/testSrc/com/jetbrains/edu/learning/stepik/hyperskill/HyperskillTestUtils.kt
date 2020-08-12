package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillUserInfo
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

const val TEST_HYPERSKILL_PROJECT_NAME = "Test Hyperskill Project"

fun testStageName(index: Int): String = "Test Stage $index"

fun loginFakeUser() {
  val fakeToken = TokenInfo().apply { accessToken = "faketoken" }
  HyperskillSettings.INSTANCE.account = HyperskillAccount().apply {
    userInfo = HyperskillUserInfo()
    userInfo.id = 1
    tokenInfo = fakeToken
  }
}

fun EduTestCase.hyperskillCourseWithFiles(projectId: Int? = 1, buildCourse: CourseBuilder.() -> Unit): HyperskillCourse {
  val course = courseWithFiles(name = TEST_HYPERSKILL_PROJECT_NAME,
                               courseProducer = ::HyperskillCourse,
                               language = FakeGradleBasedLanguage,
                               buildCourse = buildCourse) as HyperskillCourse
  course.init(projectId)
  return course
}

@Suppress("unused") // want this method to be available only in EduTestCase
fun EduTestCase.hyperskillCourse(projectId: Int? = 1, buildCourse: CourseBuilder.() -> Unit): HyperskillCourse {
  val course = course(name = TEST_HYPERSKILL_PROJECT_NAME,
                      courseProducer = ::HyperskillCourse,
                      language = FakeGradleBasedLanguage,
                      buildCourse = buildCourse) as HyperskillCourse
  course.init(projectId)
  return course
}

private fun HyperskillCourse.init(projectId: Int?) {
  if (projectId == null) {
    return
  }
  hyperskillProject = HyperskillProject().apply {
    id = projectId
    language = FakeGradleBasedLanguage.id
    title = TEST_HYPERSKILL_PROJECT_NAME
  }
  val projectLesson = getProjectLesson()
  if (projectLesson != null) {
    stages = projectLesson.items.mapIndexed { i, task ->
      HyperskillStage(i + 1, testStageName(i + 1), task.id)
    }
  }
}