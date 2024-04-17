package com.jetbrains.edu.learning.coursera

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.MissingNode
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CourseraCourse
import org.junit.Test


class SubmissionFormatTest : EduTestCase() {
  private val MAPPER by lazy { ObjectMapper() }

  @Test
  fun `test submission format`() {
    val assignmentKey = "cba"
    val taskFilePath = "src/task.txt"

    courseWithFiles(courseProducer = ::CourseraCourse) {
      lesson {
        eduTask {
          taskFile(CourseraTaskChecker.PART_ID, "abc", visible = false)
          taskFile(CourseraTaskChecker.ASSIGNMENT_KEY, assignmentKey, visible = false)
          taskFile(taskFilePath, "text")
        }
      }
    }

    val courseraSettings = CourseraSettings.getInstance()
    courseraSettings.email = "my-email@email.com"
    val token = "xyz"

    val submission = CourseraTaskChecker().createSubmissionJson(project, findTask(0, 0), courseraSettings, token)
    val submissionNode = MAPPER.readTree(submission)
    checkJsonValues(submissionNode, mapOf(CourseraTaskChecker.ASSIGNMENT_KEY to assignmentKey,
                                          "submitterEmail" to courseraSettings.email,
                                          "secret" to token))
    assertEquals(mapOf(taskFilePath to "dGV4dA=="), submissionNode.files)
  }

  private fun checkJsonValues(jsonNode: JsonNode, expectedValues: Map<String, String>) {
    for ((name, expectedValue) in expectedValues) {
      val actualValue = jsonNode.get(name)
      assertNotNull("$name wasn't found in json", actualValue)
      assertEquals(expectedValue, actualValue.asText())
    }
  }


  private val JsonNode.files: Map<String, String>
    get() {
      val outputNode = this.path("parts").path("abc").path("output")
      if (outputNode is MissingNode) {
        error("No `output` in submission")
      }
      return MAPPER.readValue(outputNode?.asText(), object : TypeReference<Map<String, String>>() {})
    }
}