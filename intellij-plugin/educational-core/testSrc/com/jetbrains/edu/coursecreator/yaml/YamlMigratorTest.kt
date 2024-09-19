package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeCourse
import com.jetbrains.edu.learning.yaml.YamlMapper.MAPPER
import com.jetbrains.edu.learning.yaml.YamlMigrator
import org.junit.Test

class YamlMigratorTest : EduTestCase() {

  private fun doTest(beforeMigration: String, afterMigration: String, toVersion: Int) {
    val migrator = YamlMigrator.getInstance(project, beforeMigration)
    val course = MAPPER.deserializeCourse(beforeMigration)
    migrator?.updateModelToVersion(toVersion, course)
    val actualMigrationResult = MAPPER.writeValueAsString(course)
    //MAPPER always writes the last YAML version
    val actualMigrationResultWithoutVersion = actualMigrationResult
      .replace("yaml_version: \\d+".toRegex(), "yaml_version: *")
      .trim()
    assertEquals(afterMigration, actualMigrationResultWithoutVersion)
  }

  @Test
  fun `migration from 0 to 1 version`() {
    doTest(
      """
        type: marketplace
        title: A test course
        language: English
        summary: no summary
        programming_language: Python
        environment: unittest
        content:
        - Lesson 1
        - Lesson 2
      """.trimIndent(),
      """
        type: marketplace
        title: A test course
        language: English
        summary: no summary
        programming_language: Python
        environment: unittest
        content:
        - Lesson 1
        - Lesson 2
        yaml_version: *
      """.trimIndent(),
      1
    )
  }
}