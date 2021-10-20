package com.jetbrains.edu.go.hyperskill

import com.goide.GoLanguage
import com.goide.sdk.GoSdk
import com.intellij.lang.Language
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.go.GoConfigurator.Companion.GO_MOD
import com.jetbrains.edu.go.GoConfigurator.Companion.MAIN_GO
import com.jetbrains.edu.go.GoProjectSettings
import com.jetbrains.edu.learning.EduExperimentalFeatures.HYPERSKILL_GO_SUPPORT
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.withFeature

class GoHyperskillNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = GoLanguage.INSTANCE
  override val settings: Any get() = GoProjectSettings(GoSdk.NULL)
  override val courseProducer: () -> Course = ::HyperskillCourse

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withFeature(HYPERSKILL_GO_SUPPORT, true) {
      super.runTestRunnable(context)
    }
  }

//  fun `test create edu task`() = checkEduTaskCreation(
//    fullTaskStructure = {
//      file(GO_MOD)
//      file(MAIN_GO)
//      file("task.html")
//    },
//    taskStructureWithoutSources = {
//      file("task.html")
//    }
//  )

  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file(GO_MOD)
      file(MAIN_GO)
      file("task.html")
      file("output.txt")
    },
    taskStructureWithoutSources = {
      file("task.html")
      file("output.txt")
    }
  )

  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file(GO_MOD)
      file(MAIN_GO)
      file("task.html")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file(GO_MOD)
      file(MAIN_GO)
      file("task.html")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file(GO_MOD)
      file(MAIN_GO)
      file("task.html")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
