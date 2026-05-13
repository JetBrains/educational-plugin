package com.jetbrains.edu.learning.ai.completion

import com.intellij.ml.inline.completion.impl.configuration.MLCompletionPerProjectSuppressor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.featureManagement.EduFeatureManager
import com.jetbrains.edu.learning.featureManagement.EduManagedFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
internal class AiCompletionFeatureWatcher(private val project: Project, private val scope: CoroutineScope) {
  private val isLaunched = AtomicBoolean(false)

  fun observeAiCompletionFeature(course: Course) {
    if (isLaunched.compareAndSet(false, true)) {
      scope.launch {
        project.service<EduFeatureManager>().disabledFeatures.collectLatest { disabledFeatures ->
          if (EduManagedFeature.AI_COMPLETION in disabledFeatures) {
            disableAiCompletion(project, course)
          }
          else {
            enableAiCompletion(project, course)
          }
        }
      }
    }
  }

  companion object {
    internal val AI_COMPLETION_SUPPRESSOR_TOKEN = MLCompletionPerProjectSuppressor.Token("JetBrains Academy")

    fun disableAiCompletion(project: Project, course: Course) {
      if (course.courseMode == CourseMode.EDUCATOR) return
      if (MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed()) return

      MLCompletionPerProjectSuppressor.getInstance(project).suppress(AI_COMPLETION_SUPPRESSOR_TOKEN)
    }

    fun enableAiCompletion(project: Project, course: Course) {
      if (course.courseMode == CourseMode.EDUCATOR) return
      if (!MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed()) return

      MLCompletionPerProjectSuppressor.getInstance(project).unsuppress(AI_COMPLETION_SUPPRESSOR_TOKEN)
    }
  }
}