package com.jetbrains.edu.aiHints.kotlin

import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.aiHints.core.context.TaskHintsDataHolder
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import org.junit.Test

/**
 * Test [com.jetbrains.edu.aiHints.core.context.AuthorSolutionContextProjectActivity] for Python language.
 */
class KtAuthorSolutionContextProjectActivityTest : JvmCourseGenerationTestBase() {
  @Test
  fun `test author solution context project activity during project initialization`() {
    // when
    val course = createKotlinCourse().apply { isMarketplace = true }

    // then start a project set up, but don't wait it to configure
    // this way we don't expect it to be in smart mode right away
    createCourseStructure(course, waitForProjectConfiguration = false)

    // verify AuthorSolutionContext has been initialized
    PlatformTestUtil.waitWhileBusy {
      TaskHintsDataHolder.getInstance(project).getTaskHintData(course.allTasks.first())?.authorSolutionContext == null
    }
  }
}