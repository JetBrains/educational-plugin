package com.jetbrains.edu.coursecreator.validation

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValidationResultNodeSerializationTest {

  @OptIn(ExperimentalSerializationApi::class)
  private val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
  }

  @Test
  fun `serialization test`() {
    assertEquals(validationResultJson, json.encodeToString(validationResult))
  }

  @Test
  fun `deserialization test`() {
    assertEquals(validationResult, json.decodeFromString<ValidationSuite>(validationResultJson))
  }

  companion object {
    private val validationResult: ValidationSuite = runBlocking {
      withValidationTreeBuilder(ValidationResultNode.ROOT_NODE_NAME) {
        validationSuit("courseSuite") {
          validationSuit("lessonSuite") {
            validationSuit("taskSuite") {
              validationCase("case1", ValidationCaseResult.Success)
              validationCase("case2", ValidationCaseResult.Ignored("ignored message"))
              validationCase("case3", ValidationCaseResult.Failed("failed message"))
              validationCase("case4", ValidationCaseResult.Failed("failed message", "failed details"))
              validationCase("case5", ValidationCaseResult.Failed("failed message", "failed details", ValidationDiff("expected", "actual")))
            }
          }
        }
      }
    }

    // language=json
    private val validationResultJson = """
    {
      "name": "root_node",
      "children": [
        {
          "type": "suite",
          "name": "courseSuite",
          "children": [
            {
              "type": "suite",
              "name": "lessonSuite",
              "children": [
                {
                  "type": "suite",
                  "name": "taskSuite",
                  "children": [
                    {
                      "type": "case",
                      "name": "case1",
                      "result": {
                        "type": "success"
                      }
                    },
                    {
                      "type": "case",
                      "name": "case2",
                      "result": {
                        "type": "ignored",
                        "message": "ignored message"
                      }
                    },
                    {
                      "type": "case",
                      "name": "case3",
                      "result": {
                        "type": "failed",
                        "message": "failed message"
                      }
                    },
                    {
                      "type": "case",
                      "name": "case4",
                      "result": {
                        "type": "failed",
                        "message": "failed message",
                        "details": "failed details"
                      }
                    },
                    {
                      "type": "case",
                      "name": "case5",
                      "result": {
                        "type": "failed",
                        "message": "failed message",
                        "details": "failed details",
                        "diff": {
                          "expected": "expected",
                          "actual": "actual"
                        }
                      }
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    }
    """.trimIndent()
  }
}
