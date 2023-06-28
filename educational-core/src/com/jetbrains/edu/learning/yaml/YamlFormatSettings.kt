package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.guessCourseDir
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG

object YamlFormatSettings {

  fun Project.isEduYamlProject() = guessCourseDir()?.findChild(COURSE_CONFIG) != null

  // it is here because it's used in test and main code
  val YAML_TEST_PROJECT_READY = Key<Boolean>("EDU.yaml_test_project_ready")
  val YAML_TEST_THROW_EXCEPTION = Key<Boolean>("EDU.yaml_test_throw_exception")

  fun shouldCreateConfigFiles(project: Project): Boolean = !isUnitTestMode || project.getUserData(YAML_TEST_PROJECT_READY) == true
}
