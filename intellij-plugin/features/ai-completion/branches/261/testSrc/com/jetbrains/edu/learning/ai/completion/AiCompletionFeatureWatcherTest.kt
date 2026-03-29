package com.jetbrains.edu.learning.ai.completion

import com.intellij.ml.inline.completion.impl.configuration.MLCompletionPerProjectSuppressor
import com.intellij.openapi.util.Disposer
import com.intellij.platform.util.coroutines.childScope
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.featureManagement.EduFeatureManager
import com.jetbrains.edu.learning.featureManagement.EduManagedFeature
import kotlinx.coroutines.*
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiCompletionFeatureWatcherTest : EduTestCase() {

  override fun tearDown() {
    try {
      MLCompletionPerProjectSuppressor.getInstance(project).unsuppress(AI_COMPLETION_SUPPRESSOR_TOKEN)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  @Test
  fun `test ai completion watcher reacts to feature management changes`() = runTest {
    val course = courseWithFiles(courseMode = CourseMode.STUDENT) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }

    val watcherScope = GlobalScope.childScope("AiCompletionFeatureWatcher-scope", Dispatchers.Unconfined)
    Disposer.register(testRootDisposable) {
      watcherScope.cancel()
    }
    val watcher = AiCompletionFeatureWatcher(project, watcherScope)

    watcher.observeAiCompletionFeature(course)
    advanceUntilIdle()

    val featureManager = EduFeatureManager.getInstance(project)

    featureManager.updateManagerState(setOf(EduManagedFeature.AI_COMPLETION))
    advanceUntilIdle()
    assertTrue(MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed())

    featureManager.updateManagerState(emptySet())
    advanceUntilIdle()
    assertFalse(MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed())
  }
}