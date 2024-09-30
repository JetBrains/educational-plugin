package com.jetbrains.edu.coursecreator.yaml

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.intellij.lang.Language
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeCourse
import com.jetbrains.edu.learning.yaml.YamlLoader
import com.jetbrains.edu.learning.yaml.YamlMapper.basicMapper
import com.jetbrains.edu.learning.yaml.YamlTestCase
import com.jetbrains.edu.learning.yaml.deserializeItemProcessingErrors
import com.jetbrains.edu.learning.yaml.errorHandling.InvalidYamlFormatException
import org.junit.Test

class YamlErrorProcessingTest : YamlTestCase() {

  @Test
  fun `test empty field`() {
    doTest("""
            |title:
            |language: Russian
            |summary: |-
            |  This is a course about string theory.
            |  Why not?"
            |programming_language: Plain text
            |content:
            |- the first lesson
            |- the second lesson
            |""".trimMargin(), YamlConfigSettings.COURSE_CONFIG,
           "title is empty", MissingKotlinParameterException::class.java)
  }

  @Test
  fun `test invalid field value`() {
    doTest("""
            |title: Test course
            |language: wrong
            |summary: |-
            |  This is a course about string theory.
            |  Why not?"
            |programming_language: Plain text
            |content:
            |- the first lesson
            |- the second lesson
            |""".trimMargin(), YamlConfigSettings.COURSE_CONFIG,
           "Unknown language \"wrong\"", InvalidYamlFormatException::class.java)
  }

  @Test
  fun `test unexpected symbol`() {
    @Suppress("DEPRECATION")
    doTest("""
            |title: Test course
            |language: Russian
            |summary: |-
            |  This is a course about string theory.
            |  Why not?"
            |programming_language: Plain text
            |content:e
            |- the first lesson
            |- the second lesson
            |""".trimMargin(), YamlConfigSettings.COURSE_CONFIG,
           "could not find expected ':' at line 7",
           MarkedYAMLException::class.java)
  }

  @Test
  fun `test parameter name without semicolon`() {
    doTest("""
            |title
            |language: Russian
            |summary: |-
            |  This is a course about string theory.
            |  Why not?"
            |programming_language: Plain text
            |content:
            |- the first lesson
            |""".trimMargin(), YamlConfigSettings.COURSE_CONFIG,
           "Invalid config", MismatchedInputException::class.java)
  }

  @Test
  fun `test wrong type of placeholder offset`() {
    doTest("""
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: a
    |    length: 3
    |    placeholder_text: type here
    |""".trimMargin(), YamlConfigSettings.TASK_CONFIG,
           "Invalid config", InvalidFormatException::class.java)
  }

  @Test
  fun `test unexpected item type`() {
    doTest("""
      |type: e
      |files:
      |- name: Test.java
      |  visible: true
      |is_multiple_choice: false
      |options:
      |- text: 1
      |  is_correct: true
      |- text: 2
      |  is_correct: false
      |""".trimMargin(), YamlConfigSettings.TASK_CONFIG,
           "Unsupported task type \"e\"", InvalidYamlFormatException::class.java)
  }

  @Test
  fun `test task without type`() {
    doTest("""
    """.trimIndent(), YamlConfigSettings.TASK_CONFIG,
           "Task type is not specified", InvalidYamlFormatException::class.java)
  }

  @Test
  fun `test negative placeholder length`() {
    doTest("""
    |type: edu
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 2
    |    length: -1
    |    placeholder_text: type here
    |""".trimMargin(), YamlConfigSettings.TASK_CONFIG,
           "Answer placeholder with negative length is not allowed", InvalidYamlFormatException::class.java)
  }

  @Test
  fun `test negative placeholder offset`() {
    doTest("""
    |type: edu
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: -1
    |    length: 1
    |    placeholder_text: type here
    |""".trimMargin(), YamlConfigSettings.TASK_CONFIG,
           "Answer placeholder with negative offset is not allowed", InvalidYamlFormatException::class.java)
  }

  @Test
  fun `test task file without name`() {
    doTest("""
    |type: edu
    |files:
    |- name:
    |  visible: true
    |""".trimMargin(), YamlConfigSettings.TASK_CONFIG,
           "File without a name is not allowed", InvalidYamlFormatException::class.java)
  }

  @Test
  fun `test language without configurator`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "HTML"
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"

    // check language is registered
    assertNotNull(Language.getRegisteredLanguages().find {it.displayName == programmingLanguage})

    // check exception as there's no configurator for this language
    assertThrows(ValueInstantiationException::class.java, ThrowableRunnable<ValueInstantiationException> {
        val yamlContent = """
      |title: $name
      |language: $language
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: $programmingLanguage
      |content:
      |- $firstLesson
      |- $secondLesson
      |""".trimMargin()
        basicMapper().deserializeCourse(yamlContent)
    })
  }

  private fun <T : Exception> doTest(yamlContent: String,
                                     configName: String,
                                     expectedErrorMessage: String,
                                     expectedExceptionClass: Class<T>) {
    try {
      val configFile = createConfigFile(configName, yamlContent)
      deserializeItemProcessingErrors(configFile, project)
    }
    catch (e: Exception) {
      assertInstanceOf(e, YamlLoader.ProcessedException::class.java)
      assertInstanceOf(e.cause, expectedExceptionClass)
      assertEquals(expectedErrorMessage, e.message)
      return
    }

    fail("Exception wasn't thrown")
  }

  private fun createConfigFile(configName: String, yamlContent: String): LightVirtualFile {
    val configFile = LightVirtualFile(configName)
    runWriteAction { VfsUtil.saveText(configFile, yamlContent) }
    return configFile
  }
}