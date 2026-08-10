package com.jetbrains.edu.coursecreator.yaml

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException
import com.fasterxml.jackson.module.kotlin.KotlinInvalidNullException
import com.intellij.lang.Language
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeCourse
import com.jetbrains.edu.learning.yaml.YamlLoader
import com.jetbrains.edu.learning.yaml.YamlMapper.basicMapper
import com.jetbrains.edu.learning.yaml.YamlTestCase
import com.jetbrains.edu.learning.yaml.deserializeItemProcessingErrors
import com.jetbrains.edu.learning.yaml.errorHandling.InvalidYamlFormatException
import org.junit.Test
import kotlin.test.assertIs

class YamlErrorProcessingTest : YamlTestCase() {

  @Test
  fun `test empty field`() {
    doTest<KotlinInvalidNullException>(YamlConfigSettings.COURSE_CONFIG, """
      title:
      language: Russian
      summary: |-
        This is a course about string theory.
        Why not?"
      programming_language: Plain text
      content:
      - the first lesson
      - the second lesson
    """, expectedErrorMessage = "title is empty")
  }

  @Test
  fun `test invalid field value`() {
    doTest<InvalidYamlFormatException>(YamlConfigSettings.COURSE_CONFIG, """
      title: Test course
      language: wrong
      summary: |-
        This is a course about string theory.
        Why not?"
      programming_language: Plain text
      content:
      - the first lesson
      - the second lesson
    """, expectedErrorMessage = "Unknown language \"wrong\"")
  }

  @Suppress("DEPRECATION")
  @Test
  fun `test unexpected symbol`() {
    doTest<MarkedYAMLException>(YamlConfigSettings.COURSE_CONFIG, """
      title: Test course
      language: Russian
      summary: |-
        This is a course about string theory.
        Why not?"
      programming_language: Plain text
      content:e
      - the first lesson
      - the second lesson
    """, expectedErrorMessage = "could not find expected ':' at line 7")
  }

  @Test
  fun `test parameter name without semicolon`() {
    doTest<MismatchedInputException>(YamlConfigSettings.COURSE_CONFIG, """
      title
      language: Russian
      summary: |-
        This is a course about string theory.
        Why not?"
      programming_language: Plain text
      content:
      - the first lesson
    """, expectedErrorMessage = "Invalid config")
  }

  @Test
  fun `test wrong type of placeholder offset`() {
    doTest<InvalidFormatException>(YamlConfigSettings.TASK_CONFIG, """
      type: edu
      files:
      - name: Test.java
        placeholders:
        - offset: a
          length: 3
          placeholder_text: type here
    """, expectedErrorMessage = "Invalid config")
  }

  @Test
  fun `test unexpected item type`() {
    doTest<InvalidYamlFormatException>(YamlConfigSettings.TASK_CONFIG, """
      type: e
      files:
      - name: Test.java
        visible: true
      is_multiple_choice: false
      options:
      - text: 1
        is_correct: true
      - text: 2
        is_correct: false
    """, expectedErrorMessage = "Unsupported task type \"e\"")
  }

  @Test
  fun `test task without type`() {
    doTest<InvalidYamlFormatException>(YamlConfigSettings.TASK_CONFIG, "", expectedErrorMessage = "Task type is not specified")
  }

  @Test
  fun `test negative placeholder length`() {
    doTest<InvalidYamlFormatException>(YamlConfigSettings.TASK_CONFIG, """
      type: edu
      files:
      - name: Test.java
        visible: true
        placeholders:
        - offset: 2
          length: -1
          placeholder_text: type here
    """, expectedErrorMessage = "Answer placeholder with negative length is not allowed")
  }

  @Test
  fun `test negative placeholder offset`() {
    doTest<InvalidYamlFormatException>(YamlConfigSettings.TASK_CONFIG, """
      type: edu
      files:
      - name: Test.java
        visible: true
        placeholders:
        - offset: -1
          length: 1
          placeholder_text: type here
    """, expectedErrorMessage = "Answer placeholder with negative offset is not allowed")
  }

  @Test
  fun `test task file without name`() {
    doTest<InvalidYamlFormatException>(YamlConfigSettings.TASK_CONFIG, """
      type: edu
      files:
      - name:
        visible: true
    """, expectedErrorMessage = "File without a name is not allowed")
  }

  @Test(expected = ValueInstantiationException::class)
  fun `test language without configurator`() {
    val programmingLanguage = "HTML"

    // check language is registered
    assertNotNull(Language.getRegisteredLanguages().find { it.displayName == programmingLanguage })

    // check exception as there's no configurator for this language
    // language=YAML
    val yamlContent = """
      title: Test Course
      language: Russian
      summary: |-
        This is a course about string theory.
        Why not?"
      programming_language: $programmingLanguage
      content:
      - the first lesson
      - the second lesson
    """.trimIndent()
    basicMapper().deserializeCourse(yamlContent)
  }

  private inline fun <reified T : Exception> doTest(
    configName: String,
    @org.intellij.lang.annotations.Language("YAML") yamlContent: String,
    expectedErrorMessage: String
  ) {
    try {
      val configFile = createConfigFile(configName, yamlContent.trimIndent())
      deserializeItemProcessingErrors(configFile, project)
    }
    catch (e: Exception) {
      assertIs<YamlLoader.ProcessedException>(e)
      assertIs<T>(e.cause)
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
