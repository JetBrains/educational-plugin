package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.Experiments
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseDir

object YamlFormatSettings {
  const val COURSE_CONFIG = "course-info.yaml"
  const val SECTION_CONFIG = "section-info.yaml"
  const val LESSON_CONFIG = "lesson-info.yaml"
  const val TASK_CONFIG = "task-info.yaml"

  fun isDisabled() = !Experiments.isFeatureEnabled(EduExperimentalFeatures.YAML_FORMAT)

  @JvmStatic
  fun Project.isEduYamlProject() = !isDisabled() && courseDir.findChild(COURSE_CONFIG) != null
}
