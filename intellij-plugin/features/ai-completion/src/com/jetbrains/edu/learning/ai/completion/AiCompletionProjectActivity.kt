package com.jetbrains.edu.learning.ai.completion

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.isHeadlessEnvironment
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("CompanionObjectInExtension")
class AiCompletionProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (isHeadlessEnvironment && !isEnabledInTests.get()) return
    val course = StudyTaskManager.getInstance(project).course ?: return
    updateAiCompletion(project, course)
  }

  companion object
}

private val isEnabledInTests = AtomicBoolean(false)

@TestOnly
fun AiCompletionProjectActivity.Companion.enableActivityInTests(disposable: Disposable) {
  isEnabledInTests.set(true)
  Disposer.register(disposable) {
    isEnabledInTests.set(false)
  }
}
