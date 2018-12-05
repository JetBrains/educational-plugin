package com.jetbrains.edu.coursecreator.actions.placeholder

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile

class CCDeleteAllPlaceholdersActionTest : EduTestCase() {

  fun `test not available in student mode`() = doTest("Foo.kt", false, EduNames.STUDY)
  fun `test not available without placeholders`() = doTest("Bar.kt", false)
  fun `test delete all placeholders`() = doTest("Foo.kt", true)

  private fun doTest(taskFileName: String, shouldBeAvailable: Boolean, courseMode: String = CCUtils.COURSE_MODE) {
    val course = courseWithFiles(courseMode = courseMode) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Foo.kt", """fun foo(): String = <p>"Foo"</p>""")
          taskFile("Bar.kt", """fun bar(): String = "Bar"""")
        }
      }
    }
    val taskFile = course.findTask("lesson1", "task1").getTaskFile(taskFileName) ?: error("Failed to find `$taskFileName` task file")
    val file = taskFile.getVirtualFile(project) ?: error("Failed to find virtual files for `$taskFileName` task file")

    myFixture.configureFromExistingVirtualFile(file)

    val presentation = myFixture.testAction(CCDeleteAllAnswerPlaceholdersAction())

    assertEquals(shouldBeAvailable, presentation.isEnabledAndVisible)
    if (shouldBeAvailable) {
      assertTrue("${CCDeleteAllAnswerPlaceholdersAction::class.java.simpleName} should delete all placeholdes",
                 taskFile.answerPlaceholders.isEmpty())
    }
  }
}
