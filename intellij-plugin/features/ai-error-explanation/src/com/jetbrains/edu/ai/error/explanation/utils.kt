package com.jetbrains.edu.ai.error.explanation

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.isMarketplaceStudentCourse

fun isErrorExplanationEnabled(project: Project): Boolean {
  return isFeatureEnabled(EduExperimentalFeatures.ERROR_EXPLANATION) && project.isMarketplaceStudentCourse()
}