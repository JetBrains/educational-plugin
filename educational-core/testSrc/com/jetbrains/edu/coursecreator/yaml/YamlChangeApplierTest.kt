package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.configFileName
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import junit.framework.TestCase


class YamlChangeApplierTest : YamlTestCase() {
  override fun setUp() {
    super.setUp()
    project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, false)
  }

  fun `test coursera submit manually`() {
    val course = courseWithFiles(courseProducer = ::CourseraCourse, courseMode = CCUtils.COURSE_MODE) {
      lesson { }
    } as CourseraCourse
    TestCase.assertFalse(course.submitManually)

    val yamlContent = """
      |type: ${CourseraNames.COURSE_TYPE}
      |submit_manually: true
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |content:
      |- lesson1
      |""".trimMargin("|")

    loadItemFromConfig(course, yamlContent)
    TestCase.assertTrue(course.submitManually)
  }
}