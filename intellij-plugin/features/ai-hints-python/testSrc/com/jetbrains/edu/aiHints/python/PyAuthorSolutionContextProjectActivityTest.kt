package com.jetbrains.edu.aiHints.python

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.aiHints.core.context.TaskHintsDataHolder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings
import com.jetbrains.python.PythonLanguage
import org.junit.Test

/**
 * Test [com.jetbrains.edu.aiHints.core.context.AuthorSolutionContextProjectActivity] for Python language.
 */
class PyAuthorSolutionContextProjectActivityTest : CourseGenerationTestBase<PyProjectSettings>() {
  override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable?>) {
    // BACKCOMPAT: Remove when this test is fixed for 2025.2
    if (ApplicationInfo.getInstance().build < BuildNumber.fromString("252")!!) {
      super.runTestRunnable(testRunnable)
    }
  }

  override val defaultSettings: PyProjectSettings = PyProjectSettings()

  @Test
  fun `test author solution context project activity during project initialization`() {
    // when
    val course = course(language = PythonLanguage.INSTANCE) {
      lesson {
        eduTask {
          taskFile("task.py", "def foo():\n    print(\"42\")")
        }
      }
    }.apply { isMarketplace = true }

    // then start a project set up, but don't wait it to configure
    // this way we don't expect it to be in smart mode right away
    createCourseStructure(course, waitForProjectConfiguration = false)

    // verify AuthorSolutionContext has been initialized
    PlatformTestUtil.waitWhileBusy {
      TaskHintsDataHolder.getInstance(project).getTaskHintData(course.allTasks.first())?.authorSolutionContext == null
    }
  }
}