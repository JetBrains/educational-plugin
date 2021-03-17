package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.lang.Language
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProfileInfo
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

const val TEST_HYPERSKILL_PROJECT_NAME = "Test Hyperskill Project"

fun testStageName(index: Int): String = "Test Stage $index"

fun loginFakeUser() {
  val fakeToken = TokenInfo().apply { accessToken = "faketoken" }
  HyperskillSettings.INSTANCE.account = HyperskillAccount().apply {
    userInfo = HyperskillProfileInfo()
    userInfo.id = 1
    tokenInfo = fakeToken
  }
}

fun EduTestCase.hyperskillCourseWithFiles(
  projectId: Int? = 1,
  name: String = TEST_HYPERSKILL_PROJECT_NAME,
  language: Language = FakeGradleBasedLanguage,
  courseMode: String = EduNames.STUDY,
  completeStages: Boolean = false,
  buildCourse: CourseBuilder.() -> Unit
): HyperskillCourse {
  val course = courseWithFiles(name = name, courseProducer = ::HyperskillCourse, courseMode = courseMode, language = language,
                               buildCourse = buildCourse) as HyperskillCourse
  course.init(projectId, completeStages)
  return course
}

@Suppress("unused") // want this method to be available only in EduTestCase
fun EduTestCase.hyperskillCourse(
  projectId: Int? = 1,
  language: Language = FakeGradleBasedLanguage,
  completeStages: Boolean = false,
  buildCourse: CourseBuilder.() -> Unit
): HyperskillCourse {
  val course = course(name = TEST_HYPERSKILL_PROJECT_NAME,
                      courseProducer = ::HyperskillCourse,
                      language = language,
                      buildCourse = buildCourse) as HyperskillCourse
  course.init(projectId, completeStages)
  return course
}

@Suppress("unused") // want this method to be available only in EduTestCase
fun EduTestCase.defaultHyperskillCourse(): HyperskillCourse {
  return hyperskillCourse {
    frameworkLesson {
      eduTask("task1", stepId = 1) {
        taskFile("src/Task.kt", "stage 1")
        taskFile("test/Tests1.kt", "stage 1 test")
      }
      eduTask("task2", stepId = 2) {
        taskFile("src/Task.kt", "stage 2")
        taskFile("test/Tests2.kt", "stage 2 test")
      }
    }
  }
}

private fun HyperskillCourse.init(projectId: Int?, completeStages: Boolean) {
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
      HyperskillStage(i + 1, testStageName(i + 1), task.id, isStageCompleted = completeStages)
    }
  }
}
