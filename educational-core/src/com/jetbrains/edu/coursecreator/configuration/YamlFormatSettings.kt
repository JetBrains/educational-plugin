package com.jetbrains.edu.coursecreator.configuration

import com.intellij.openapi.application.Experiments
import com.jetbrains.edu.learning.EduExperimentalFeatures

object YamlFormatSettings {
  const val COURSE_CONFIG = "course-info.yaml"
  const val SECTION_CONFIG = "section-info.yaml"
  const val LESSON_CONFIG = "lesson-info.yaml"
  const val TASK_CONFIG = "task-info.yaml"

  fun isDisabled() = !Experiments.isFeatureEnabled(EduExperimentalFeatures.YAML_FORMAT)
}
