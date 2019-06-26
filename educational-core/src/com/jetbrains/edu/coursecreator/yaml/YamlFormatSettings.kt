package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.isUnitTestMode

object YamlFormatSettings {
  val COURSE_CONFIG = getLocalConfigFileName(EduNames.COURSE)
  val SECTION_CONFIG = getLocalConfigFileName(EduNames.SECTION)
  val LESSON_CONFIG = getLocalConfigFileName(EduNames.LESSON)
  val TASK_CONFIG = getLocalConfigFileName(EduNames.TASK)

  /**
   * @param itemKind Course/Section/Lesson/Task
   */
  fun getLocalConfigFileName(itemKind: String): String = "$itemKind-info.yaml"

  const val REMOTE_COURSE_CONFIG = "course-remote-info.yaml"
  const val REMOTE_SECTION_CONFIG = "section-remote-info.yaml"
  const val REMOTE_LESSON_CONFIG = "lesson-remote-info.yaml"
  const val REMOTE_TASK_CONFIG = "task-remote-info.yaml"

  @JvmStatic
  fun Project.isEduYamlProject() = courseDir.findChild(COURSE_CONFIG) != null

  // it is here because it's used in test and main code
  val YAML_TEST_PROJECT_READY = Key<Boolean>("EDU.yaml_test_project_ready")
  val YAML_TEST_THROW_EXCEPTION = Key<Boolean>("EDU.yaml_test_throw_exception")

  fun shouldCreateConfigFiles(project: Project): Boolean = !isUnitTestMode || project.getUserData(YAML_TEST_PROJECT_READY) == true
}
