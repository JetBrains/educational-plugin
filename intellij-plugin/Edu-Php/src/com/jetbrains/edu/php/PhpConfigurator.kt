package com.jetbrains.edu.php

import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.php.composer.ComposerUtils
import javax.swing.Icon

class PhpConfigurator : EduConfigurator<PhpProjectSettings> {

  override val courseBuilder: EduCourseBuilder<PhpProjectSettings>
    get() = PhpCourseBuilder()

  override val testFileName: String
    get() = TEST_PHP

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PhpTaskCheckerProvider()

  override val logo: Icon
    // the default icon from plugin looks ugly, so we use ours
    get() = EducationalCoreIcons.Language.Php

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val sourceDir: String
    get() = EduNames.SRC

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("com.jetbrains.php"))

  override fun getMockFileName(course: Course, text: String): String = TASK_PHP

  override fun isTestFile(task: Task, path: String): Boolean = super.isTestFile(task, path) || path == testFileName

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    dirAndChildren(ComposerUtils.VENDOR_DIR_DEFAULT_NAME) {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    file(ComposerUtils.CONFIG_DEFAULT_FILENAME) {
      archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
    }

    file(ComposerUtils.COMPOSER_PHAR_NAME) {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }
  }

  companion object {
    const val MAIN_PHP = "main.php"
    const val TASK_PHP = "task.php"
    const val TEST_PHP = "test.php"
  }
}