package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.writeText
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.migration.YamlMigrator
import org.junit.Test

class YamlMigratorTest : EduTestCase() {

  private fun migrator(updateToVersion: Int): YamlMigrator {
    val migrator = YamlMigrator.getInstance(project) ?: error("Failed to find migrator")
    migrator.updateStructureToVersion(updateToVersion)
    return migrator
  }

  private fun withConfig(path: String, contents: String) {
    runInEdtAndWait {
      runWriteAction {
        val configFile = project.courseDir.findOrCreateChildData(null, path)
        configFile.writeText(contents)
      }
    }
  }

  private fun assertConfig(migrator: YamlMigrator, path: String, contents: String) {
    assertEquals(contents, migrator.migratedConfig(path))
  }

  @Test
  fun `migration from 0 to 1 version`() {
    withConfig(
      COURSE_CONFIG, """
        type: marketplace
        title: A test course
        language: English
        summary: no summary
        programming_language: Python
        environment: unittest
        content:
        - Lesson 1
        - Lesson 2
      """.trimIndent()
    )

    val migrator = migrator(1)

    assertConfig(
      migrator, COURSE_CONFIG, """
      type: marketplace
      title: A test course
      language: English
      summary: no summary
      programming_language: Python
      environment: unittest
      content:
      - Lesson 1
      - Lesson 2
      yaml_version: 1
      
    """.trimIndent()
    )
  }
}