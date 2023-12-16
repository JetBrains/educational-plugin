package com.jetbrains.edu.android.gradle

import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker
import com.google.wireless.android.sdk.stats.GradleSyncStats
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.GradleCourseRefresher
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.isUnitTestMode

class StudioGradleCourseRefresher : GradleCourseRefresher {
  override fun isAvailable(): Boolean = EduUtilsKt.isAndroidStudio()

  override fun refresh(project: Project, cause: RefreshCause) {
    if (cause == RefreshCause.PROJECT_CREATED && !isUnitTestMode) return

    val request = GradleSyncInvoker.Request(GradleSyncStats.Trigger.TRIGGER_PROJECT_MODIFIED)
    GradleSyncInvoker.getInstance().requestProjectSync(project, request, null)
  }
}
