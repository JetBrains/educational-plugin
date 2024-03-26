package com.jetbrains.edu.coursecreator.framework.impl

import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonRecordStorage
import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.findTask

class CCFrameworkLessonRecordStorageSerializationTest : EduSettingsServiceTestBase() {
  override fun setUp() {
    super.setUp()
    storage.reset()
  }

  fun `test empty storage serialization`() {
    storage.loadStateAndCheck("""
      <State />
    """.trimIndent())
  }

  fun `test storage serialization`() {
    val course = createFrameworkCourse()
    val task1 = course.findTask("lesson1", "task1")

    with(storage) {
      updateRecord(task1, 5)
      loadStateAndCheck("""
        <State>
          <taskRecords>
            <map>
              <entry key="lesson1/task1" value="5" />
            </map>
          </taskRecords>
        </State>
      """.trimIndent())

      updateRecord(task1, 6)
      loadStateAndCheck("""
        <State>
          <taskRecords>
            <map>
              <entry key="lesson1/task1" value="6" />
            </map>
          </taskRecords>
        </State>
      """.trimIndent())

      removeRecord(task1)
      loadStateAndCheck("""
         <State />
      """.trimIndent()
      )
    }
  }

  private fun createFrameworkCourse(): Course = courseWithFiles(
    courseMode = CourseMode.EDUCATOR,
    language = FakeGradleBasedLanguage,
  ) {
    frameworkLesson("lesson1") {
      eduTask("task1") {
        taskFile("Task.kt", "fun foo()")
      }
      eduTask("task2") {
        taskFile("Task.kt", "fun foo()")
      }
    }
  }

  private val storage: CCFrameworkLessonRecordStorage
    get() = CCFrameworkLessonRecordStorage.getInstance(project)
}