package com.jetbrains.edu.coursecreator.yaml

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile

class YamlErrorProcessingTest : YamlTestCase() {

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
            |""".trimMargin("|"), YamlFormatSettings.COURSE_CONFIG,
           "title is empty", MissingKotlinParameterException::class.java)
  }

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
            |""".trimMargin("|"), YamlFormatSettings.COURSE_CONFIG,
           "Unknown language 'wrong'", JsonMappingException::class.java)
  }

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
            |""".trimMargin("|"), YamlFormatSettings.COURSE_CONFIG,
           "could not find expected ':' at line 7",
           com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException::class.java)
  }

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
            |""".trimMargin("|"), YamlFormatSettings.COURSE_CONFIG,
           "invalid config", MismatchedInputException::class.java)
  }

  fun `test wrong type of placeholder offset`() {
    doTest("""
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: a
    |    length: 3
    |    placeholder_text: type here
    |""".trimMargin("|"), YamlFormatSettings.TASK_CONFIG,
           "invalid config", InvalidFormatException::class.java)
  }

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
      |""".trimMargin("|"), YamlFormatSettings.TASK_CONFIG,
           "Unsupported task type 'e'", InvalidYamlFormatException::class.java)
  }

  private fun <T : Exception> doTest(yamlContent: String,
                                     configName: String,
                                     expectedErrorMessage: String,
                                     expectedExceptionClass: Class<T>) {
    try {
      val configFile = createConfigFile(configName, yamlContent)
      YamlDeserializer.deserializeItem(project, configFile)
    }
    catch (e: Exception) {
      assertInstanceOf(e, YamlDeserializer.ProcessedException::class.java)
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