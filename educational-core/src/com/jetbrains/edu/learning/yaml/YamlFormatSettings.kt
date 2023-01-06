package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.guessCourseDir
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LESSON
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SECTION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TASK
import org.jetbrains.annotations.NonNls

object YamlFormatSettings {
  @JvmField
  val COURSE_CONFIG = getLocalConfigFileName(EduNames.COURSE)
  @JvmField
  val SECTION_CONFIG = getLocalConfigFileName(SECTION)
  @JvmField
  val LESSON_CONFIG = getLocalConfigFileName(LESSON)

  @JvmField
  val TASK_CONFIG = getLocalConfigFileName(TASK)

  /**
   * @param itemKind Course/Section/Lesson/Task
   */
  fun getLocalConfigFileName(itemKind: String): String = "$itemKind-info.yaml"

  fun localConfigNameToRemote(fileName: String): String = "${fileName.substringBefore("-")}-remote-info.yaml"

  @NonNls
  const val REMOTE_COURSE_CONFIG = "course-remote-info.yaml"

  @NonNls
  const val REMOTE_SECTION_CONFIG = "section-remote-info.yaml"

  @NonNls
  const val REMOTE_LESSON_CONFIG = "lesson-remote-info.yaml"

  @NonNls
  const val REMOTE_TASK_CONFIG = "task-remote-info.yaml"

  @JvmStatic
  fun Project.isEduYamlProject() = guessCourseDir()?.findChild(COURSE_CONFIG) != null

  // it is here because it's used in test and main code
  val YAML_TEST_PROJECT_READY = Key<Boolean>("EDU.yaml_test_project_ready")
  val YAML_TEST_THROW_EXCEPTION = Key<Boolean>("EDU.yaml_test_throw_exception")

  fun shouldCreateConfigFiles(project: Project): Boolean = !isUnitTestMode || project.getUserData(YAML_TEST_PROJECT_READY) == true
}
