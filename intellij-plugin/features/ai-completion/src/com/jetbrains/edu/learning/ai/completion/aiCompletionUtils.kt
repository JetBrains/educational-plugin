package com.jetbrains.edu.learning.ai.completion

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.featureManagement.EduFeatureManager
import com.jetbrains.edu.learning.featureManagement.EduManagedFeature

fun updateAiCompletion(project: Project, course: Course) {
  if (course.courseMode == CourseMode.EDUCATOR) return
  // temporary workaround: we now have 2 sources of truth about `disabledFeatures`
  // since the project activity runs before the `afterProjectGenerated`, disabled features are not yet persisted in the service
  // for the subsequent project openings the disabled features are persisted in the service and erased from course itself
  val isCompletionDisabled = course.disabledFeatures.contains(EduManagedFeature.AI_COMPLETION.featureKey)
                             || project.service<EduFeatureManager>().checkDisabled(EduManagedFeature.AI_COMPLETION)

  if (isCompletionDisabled) {
    disableAiCompletion(project)
  }
  else {
    enableAiCompletion(project)
  }
}
