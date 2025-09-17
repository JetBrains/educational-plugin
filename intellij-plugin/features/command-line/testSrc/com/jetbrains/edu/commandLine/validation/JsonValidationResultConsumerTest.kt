package com.jetbrains.edu.commandLine.validation

import org.intellij.lang.annotations.Language
import org.junit.Test

class JsonValidationResultConsumerTest : ValidationResultConsumerTestBase() {

  override fun createResultConsumer(outputConsumer: ValidationOutputConsumer): ValidationResultConsumer {
    return JsonValidationResultConsumer(outputConsumer)
  }

  @Language("JSON")
  @Test
  fun `test output`() = doTest(sampleValidationResult(), """
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
  """.trimIndent().trimEnd())
}
