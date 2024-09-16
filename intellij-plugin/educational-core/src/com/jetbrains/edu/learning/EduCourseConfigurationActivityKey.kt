package com.jetbrains.edu.learning

import com.intellij.platform.backend.observation.ActivityKey
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.Nls

@Suppress("unused")
object EduCourseConfigurationActivityKey : ActivityKey {
  override val presentableName: @Nls String
    get() = EduCoreBundle.message("course.configuration")
}
