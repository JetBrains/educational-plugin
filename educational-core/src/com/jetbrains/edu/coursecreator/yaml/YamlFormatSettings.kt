package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.isUnitTestMode

object YamlFormatSettings {
  const val COURSE_CONFIG = "course-info.yaml"
  const val SECTION_CONFIG = "section-info.yaml"
  const val LESSON_CONFIG = "lesson-info.yaml"
  const val TASK_CONFIG = "task-info.yaml"

  const val REMOTE_COURSE_CONFIG = "course-remote-info.yaml"
  const val REMOTE_SECTION_CONFIG = "section-remote-info.yaml"
  const val REMOTE_LESSON_CONFIG = "lesson-remote-info.yaml"
  const val REMOTE_TASK_CONFIG = "task-remote-info.yaml"

  fun Project.isEduYamlProject() = courseDir.findChild(COURSE_CONFIG) != null

  // it is here because it's used in test and main code
  val YAML_TEST_PROJECT_READY = Key<Boolean>("EDU.yaml_test_project_ready")

  fun shouldCreateConfigFiles(project: Project): Boolean = !isUnitTestMode || project.getUserData(YAML_TEST_PROJECT_READY) == true
}
