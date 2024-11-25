package com.jetbrains.edu.php

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import com.jetbrains.edu.php.PhpConfigurator.Companion.TASK_PHP
import com.jetbrains.edu.php.PhpConfigurator.Companion.TEST_PHP
import com.jetbrains.php.composer.ComposerUtils
import com.jetbrains.php.config.interpreters.PhpInterpreter
import com.jetbrains.php.config.interpreters.PhpInterpretersManagerImpl
import com.jetbrains.php.lang.PhpLanguage
import org.junit.Test

class PhpCourseBuilderTest : CourseGenerationTestBase<PhpProjectSettings>() {
  override val defaultSettings = PhpProjectSettings()
  override fun setUp() {
    super.setUp()
    val phpInterpreter = PhpInterpreter()
    phpInterpreter.name = "interpreterName"
    defaultSettings.phpInterpreter = phpInterpreter
    PhpInterpretersManagerImpl.getInstance(project).addInterpreter(phpInterpreter)
  }

  @Test
  fun `test new educator course`() {
    val newCourse = newCourse(PhpLanguage.INSTANCE)

    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file(TASK_PHP)
        }
        dir("test") {
          file(TEST_PHP)
        }
        file("task.md")
      }
      file(ComposerUtils.CONFIG_DEFAULT_FILENAME)
    }.assertEquals(rootDir)

    assertListOfAdditionalFiles(newCourse,
      ComposerUtils.CONFIG_DEFAULT_FILENAME to getInternalTemplateText(ComposerUtils.CONFIG_DEFAULT_FILENAME)
    )
  }

  @Test
  fun `test study course structure`() {
    val changedConfig = "{" +
                        "  \"require\": {" +
                        "    \"phpunit/phpunit\": \"^9\"" +
                        "  }" +
                        "}"
    val course = course(language = PhpLanguage.INSTANCE) {
      lesson("lesson1") {
        eduTask("task1") {
          dir("src") {
            taskFile(TASK_PHP)
          }
          dir("test") {
            taskFile(TEST_PHP)
          }
        }
      }
      additionalFiles {
        eduFile(ComposerUtils.CONFIG_DEFAULT_FILENAME, changedConfig)
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file(TASK_PHP)
        }
        dir("test") {
          file(TEST_PHP)
        }
        file("task.md")
      }
      file(ComposerUtils.CONFIG_DEFAULT_FILENAME)
    }.assertEquals(rootDir)

    // student gets config changed by a teacher
    assertListOfAdditionalFiles(course,
      ComposerUtils.CONFIG_DEFAULT_FILENAME to changedConfig
    )
  }

  @Test
  fun `composer_json is recreated for student`() {
    val course = newCourse(PhpLanguage.INSTANCE, courseMode = CourseMode.STUDENT)
    createCourseStructure(course)

    // student gets a recreated composer.json if it was not in additional files
    assertListOfAdditionalFiles(course,
      ComposerUtils.CONFIG_DEFAULT_FILENAME to getInternalTemplateText(ComposerUtils.CONFIG_DEFAULT_FILENAME)
    )
  }
}