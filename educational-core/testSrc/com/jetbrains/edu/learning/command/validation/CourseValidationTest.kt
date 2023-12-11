package com.jetbrains.edu.learning.command.validation

import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.json.psi.JsonElement
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import java.net.HttpURLConnection

class CourseValidationTest : EduTestCase() {

  override fun runInDispatchThread(): Boolean = false

  override fun tearDown() {
    @Suppress("UnstableApiUsage")
    invokeAndWaitIfNeeded {
      super.tearDown()
    }
  }

  fun `test events for check action`() {
    val course = courseWithFiles {
      lesson("lesson with unchecked tasks") {
        theoryTask("theory unchecked task") {
          checkResultFile(CheckStatus.Unchecked)
        }
        eduTask("edu unchecked task") {
          checkResultFile(CheckStatus.Unchecked)
        }
      }
      lesson("lesson with solved tasks") {
        theoryTask("theory solved task") {
          checkResultFile(CheckStatus.Solved)
        }
        eduTask("edu solved task") {
          checkResultFile(CheckStatus.Solved)
        }
        outputTask("output solved task") {
          checkResultFile("Answer")
          dir("tests") {
            taskFile("output.txt") {
              withText("Answer")
            }
          }
        }
      }
      section("section with failed tasks") {
        lesson("lesson with failed tasks") {
          theoryTask("theory failed task") {
            checkResultFile(CheckStatus.Failed)
          }
          eduTask("edu failed task") {
            checkResultFile(CheckStatus.Failed)
          }
          outputTask("output failed task") {
            checkResultFile("Answer1")
            dir("tests") {
              taskFile("output.txt") {
                withText("Answer2")
              }
            }
          }
        }
      }
    }

    doTest(course, validateTests = true, validateLinks = false, """
      - Test Course:
        - lesson with unchecked tasks:
          - theory unchecked task:
            - Tests: ignored
          - edu unchecked task:
            - Tests: ignored
        - lesson with solved tasks:
          - theory solved task:
            - Tests: success
          - edu solved task:
            - Tests: success
          - output solved task:
            - Tests: success
        - section with failed tasks:
          - lesson with failed tasks:
            - theory failed task:
              - Tests: failed
            - edu failed task:
              - Tests: failed
            - output failed task:
              - Tests: failed
    """)
  }

  fun `test link validation`() {
    val helper = MockWebServerHelper(testRootDisposable)

    val course = courseWithFiles {
      lesson("lesson1") {
        eduTask("valid links", taskDescription = """
          [http link](${helper.baseUrl}valid)
          [file link](file://foo/bar.txt)
          [course link](course://lesson1/invalid%20links/Task2.txt)
          [psi element link](psi_element://foo.bar)
          [tool window link](tool_window://Task)
          [settings link](settings://Educational)
        """.trimIndent(), taskDescriptionFormat = DescriptionFormat.MD) {
          taskFile("Task1.txt")
        }
        eduTask("invalid links", taskDescription = """
          [http link](${helper.baseUrl}invalid)
          [file link](file://foo/baz.txt)
          [course link](course://lesson1/valid%20links/Task3.txt)
          [psi element link](psi_element://foo.baz)
          [tool window link](tool_window://Task1)
          [settings link](settings://Educational2)
        """.trimIndent(), taskDescriptionFormat = DescriptionFormat.MD) {
          taskFile("Task2.txt")
        }
      }
      additionalFile("foo/bar.txt")
      // Used for psi links
      additionalFile("config.json", """{ "foo": { "bar": "" } }""")
    }

    // Make psi link resolution works for JSON.
    QualifiedNameProvider.EP_NAME.point.registerExtension(TestJsonQualifiedNameProvider(), testRootDisposable)
    registerTaskDescriptionToolWindow()
    helper.addResponseHandler(testRootDisposable) { _, path ->
      when (path) {
        "/invalid" -> MockResponseFactory.notFound()
        "/valid" -> MockResponseFactory.ok()
        else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
      }
    }

    doTest(course, validateTests = false, validateLinks = true, """
      - Test Course:
        - lesson1:
          - valid links:
            - Task description links:
              - ${helper.baseUrl}valid: success
              - file://foo/bar.txt: success
              - course://lesson1/invalid links/Task2.txt: success
              - psi_element://foo.bar: success
              - tool_window://Task: success
              - settings://Educational: success
          - invalid links:
            - Task description links:
              - ${helper.baseUrl}invalid: failed
              - file://foo/baz.txt: failed
              - course://lesson1/valid links/Task3.txt: failed
              - psi_element://foo.baz: failed
              - tool_window://Task1: failed
              - settings://Educational2: failed   
    """)
  }

  private fun doTest(course: Course, validateTests: Boolean, validateLinks: Boolean, expected: String) {
    val testMessageConsumer = TestServiceMessageConsumer()
    val params = ValidationParams(validateTests = validateTests, validateLinks = validateLinks)
    val validationHelper = CourseValidationHelper(params, testMessageConsumer)

    runBlocking {
      validationHelper.validate(project, course)
    }

    testMessageConsumer.assertTestTreeEquals(expected)
  }
}


private class TestJsonQualifiedNameProvider : QualifiedNameProvider {
  override fun adjustElementToCopy(element: PsiElement): PsiElement? = null
  override fun getQualifiedName(element: PsiElement): String? = null

  // It shouldn't be too precise, but it should be enough to check how we work with psi links
  override fun qualifiedNameToElement(fqn: String, project: Project): PsiElement? {
    val file = project.courseDir.findFile("config.json")?.findPsiFile(project) as? JsonFile ?: return null
    val segments = fqn.split(".")
    var currentElement: JsonElement = file
    for (segment in segments) {
      currentElement = currentElement
        .childrenOfType<JsonObject>()
        .singleOrNull()
        ?.childrenOfType<JsonProperty>()
        ?.find { it.name == segment }
        ?: return null
    }
    return currentElement
  }
}