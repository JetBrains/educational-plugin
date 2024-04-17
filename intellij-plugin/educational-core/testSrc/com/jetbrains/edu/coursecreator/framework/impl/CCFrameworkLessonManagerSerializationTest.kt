package com.jetbrains.edu.coursecreator.framework.impl

import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class CCFrameworkLessonManagerSerializationTest : EduSettingsServiceTestBase() {
  @Test
  fun `test empty storage serialization`() {
    CCFrameworkLessonManager.getInstance(project).loadStateAndCheck("""
      <RecordState />
    """.trimIndent())
  }

  @Test
  fun `test storage serialization`() {
    val course = createFrameworkCourse()
    val task1 = course.findTask("lesson1", "task1")

    with(CCFrameworkLessonManager.getInstance(project)) {
      updateRecord(task1, 5)
      loadStateAndCheck("""
        <RecordState>
          <taskRecords>
            <map>
              <entry key="lesson1/task1" value="5" />
            </map>
          </taskRecords>
        </RecordState>
      """.trimIndent())

      updateRecord(task1, 6)
      loadStateAndCheck("""
        <RecordState>
          <taskRecords>
            <map>
              <entry key="lesson1/task1" value="6" />
            </map>
          </taskRecords>
        </RecordState>
      """.trimIndent())

      removeRecord(task1)
      loadStateAndCheck("""
         <RecordState />
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
}