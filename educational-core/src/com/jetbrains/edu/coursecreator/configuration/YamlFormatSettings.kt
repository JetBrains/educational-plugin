package com.jetbrains.edu.coursecreator.configuration

import com.intellij.openapi.application.Experiments


object YamlFormatSettings {
  private const val FEATURE_ID = "edu.course.creator.yaml"

  const val COURSE_CONFIG = "course-info.yaml"
  const val SECTION_CONFIG = "section-info.yaml"
  const val LESSON_CONFIG = "lesson-info.yaml"
  const val TASK_CONFIG = "task-info.yaml"

  fun isDisabled() = !Experiments.isFeatureEnabled(FEATURE_ID);
}