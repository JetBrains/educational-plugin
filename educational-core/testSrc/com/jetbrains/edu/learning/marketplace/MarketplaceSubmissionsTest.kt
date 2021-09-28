package com.jetbrains.edu.learning.marketplace

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.*
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.stepik.SubmissionsTestBase
import com.jetbrains.edu.learning.withFeature
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import junit.framework.TestCase
import okhttp3.ResponseBody
import org.intellij.lang.annotations.Language
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.time.Instant
import java.util.*

class MarketplaceSubmissionsTest: SubmissionsTestBase() {

  override fun setUp() {
    super.setUp()

    val account = MarketplaceAccount()
    account.userInfo = MarketplaceUserInfo("Test User")
    account.saveJwtToken("not empty jwt token")
    MarketplaceSettings.INSTANCE.account = account
    
    courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse,
      id = 1
    ) {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
        eduTask("task1", stepId = 2) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    }.apply { isMarketplace = true } as EduCourse
    configureResponses()
  }

  private fun configureResponses() {
    val mapper = ObjectMapper()
    mockkConstructor(Retrofit::class)
    val service = mockk<SubmissionsService>()
    every {
      anyConstructed<Retrofit>().create(SubmissionsService::class.java)
    } returns service

    val createDocumentCall = mockk <Call<Document>>()
    every { service.createDocument(any()) } returns createDocumentCall
    val submissionDocResponse = mapper.treeToValue(mapper.readTree(submissionsDocument), Document::class.java)
    every { createDocumentCall.execute() } returns Response.success(submissionDocResponse)

    every { service.addPathToDocument(any()).execute() } returns Response.success(mockk())

    val updateDocumentResponse = mockk<Call<ResponseBody>>()
    every { service.updateDocument(any()) } returns updateDocumentResponse
    every { updateDocumentResponse.execute() } returns Response.success(mockk())

    val versionsListCall = mockk <Call<Versions>>()
    every { service.getVersionsList(any()) } returns versionsListCall
    val versionsResponse = mapper.treeToValue(mapper.readTree(versions), Versions::class.java)
    every { versionsListCall.execute() } returns Response.success(versionsResponse)

    val contentCall = mockk <Call<Content>>()
    every { service.getSubmissionContent(any()) } returns contentCall
    val submissionContentResponse = mapper.treeToValue(mapper.readTree(submissionContent), Content::class.java)
    every { contentCall.execute() } returns Response.success(submissionContentResponse)

    val descriptorsListCall = mockk <Call<Descriptors>>()
    every { service.getDescriptorsList(any()) } returns descriptorsListCall
    val descriptorsListResponse = mapper.treeToValue(mapper.readTree(descriptors), Descriptors::class.java)
    every { descriptorsListCall.execute() } returns Response.success(descriptorsListResponse)
  }

  fun `test submission document created after edu task check`() {
    doTestSubmissionAddedAfterTaskCheck(1, EduNames.CORRECT)
    val firstTask = findTask(0, 0)
    assertEquals("f53a097b-1486-43ae-8597-d464e74fe6a7", firstTask.submissionsId)
  }

  fun `test submission document updated after edu task check`() {
    val firstTask = findTask(0, 0)
    firstTask.submissionsId = "f53a097b-1486-43ae-8597-d464e74fe6a7"

    doTestSubmissionAddedAfterTaskCheck(1, EduNames.CORRECT)
  }

  fun `test versions list loaded for document id`() {
    val versions = MarketplaceSubmissionsConnector.getInstance().getDocVersionsIds("1")
    checkNotNull(versions){ "Versions list is null" }
    assertTrue(versions.size == 2)
    assertEquals("SlARMo.y2SqpGLAoFHHGxjNxKqo3PRf5", versions[0].id)
    assertEquals(1626965426, versions[0].timestamp)
  }

  fun `test submission loaded`() {
    val version = Version("1", 1626965426)
    val submission = MarketplaceSubmissionsConnector.getInstance().getSubmission("1", version)
    checkNotNull(submission){ "Content is null" }
    assertEquals(Date.from(Instant.ofEpochSecond(version.timestamp)), submission.time)
    assertEquals(EduNames.CORRECT, submission.status)
    assertEquals("solution text", submission.reply?.solution?.get(0)?.text)
  }

  fun `test get document id`() {
    val documentId = MarketplaceSubmissionsConnector.getInstance().getDocumentId(1, 1)
    assertEquals("b321c43b-46b9-488d-8947-10647795516a", documentId)
  }

  fun `test all submissions loaded`() {
    val course = getCourse() as EduCourse
    val submissionsByTaskId = MarketplaceSubmissionsConnector.getInstance().getAllSubmissions(course)
    val submissions = submissionsByTaskId[1]
    checkNotNull(submissions) {"Submissions list is null"}
    assertTrue(submissions.size == 2)
    val firstTask = findTask(0, 0)
    TestCase.assertEquals("b321c43b-46b9-488d-8947-10647795516a", firstTask.submissionsId)
  }

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withFeature(EduExperimentalFeatures.MARKETPLACE_SUBMISSIONS, true) {
      super.runTestRunnable(context)
    }
  }

  @Language("JSON")
  private val submissionsDocument = """
    {
       "id": "f53a097b-1486-43ae-8597-d464e74fe6a7"
    }
  """

  @Language("JSON")
  private val versions = """
{
  "versions": [
    {
      "id": "SlARMo.y2SqpGLAoFHHGxjNxKqo3PRf5",
      "timestamp": 1626965426
    },
    {
      "id": "x8JYWA8jtAL.ZGP1DXvBzmSG5gNiRLMs",
      "timestamp": 1618268872
    }
  ]
}
  """

  //correct format
  @Language("JSON")
  private val submissionContentCorrеctFormat = """
  {
    "content": "{\"submission\":{\"attempt\":0,\"reply\":{\"choices\":null,\"score\":\"1\",\"solution\":[{\"visible\":true,\"name\":\"hello_world.py\",\"text\":\"solution text\",\"is_visible\":true}],\"language\":null,\"code\":null,\"edu_task\":\"{\\\"task\\\":{\\\"name\\\":\\\"Our first program\\\",\\\"stepic_id\\\":186010,\\\"status\\\":\\\"Solved\\\",\\\"files\\\":{\\\"execute.svg\\\":{\\\"name\\\":\\\"execute.svg\\\",\\\"placeholders\\\":[],\\\"is_visible\\\":false,\\\"text\\\":\\\"<svg height=\\\\\\\"16\\\\\\\" viewBox=\\\\\\\"0 0 16 16\\\\\\\" width=\\\\\\\"16\\\\\\\" xmlns=\\\\\\\"http://www.w3.org/2000/svg\\\\\\\"><path d=\\\\\\\"m4 2 10 6-10 6z\\\\\\\" fill=\\\\\\\"#59a869\\\\\\\" fill-rule=\\\\\\\"evenodd\\\\\\\"/></svg>\\\"},\\\"hello_world.py\\\":{\\\"name\\\":\\\"hello_world.py\\\",\\\"placeholders\\\":[{\\\"offset\\\":32,\\\"length\\\":4,\\\"dependency\\\":null,\\\"hints\\\":[],\\\"possible_answer\\\":\\\"Liana\\\",\\\"placeholder_text\\\":\\\"Type your name here\\\",\\\"selected\\\":false}],\\\"is_visible\\\":true,\\\"text\\\":\\\"print(\\\\\\\"Hello, world! My name is Type your name here\\\\\\\")\\\\n\\\"},\\\"tests.py\\\":{\\\"name\\\":\\\"tests.py\\\",\\\"placeholders\\\":[],\\\"is_visible\\\":false,\\\"text\\\":\\\"from test_helper import run_common_tests, failed, passed, get_answer_placeholders\\\\n\\\\n\\\\ndef test_initial():\\\\n    window = get_answer_placeholders()[0]\\\\n    if window == \\\\\\\"type your name\\\\\\\":\\\\n        failed(\\\\\\\"You should modify the file\\\\\\\")\\\\n    else:\\\\n        passed()\\\\n\\\\nif __name__ == '__main__':\\\\n    run_common_tests(\\\\\\\"You should enter your name\\\\\\\")\\\\n    test_initial()\\\\n\\\\n\\\\n\\\"}},\\\"task_type\\\":\\\"edu\\\"}}\",\"version\":11,\"feedback\":null},\"step\":186010,\"id\":1233312,\"status\":null,\"hint\":null,\"feedback\":null,\"time\":0}}"
  }
  """

  //format with a bug, will be fixed on grazie side
  @Language("JSON")
  private val submissionContent = """
  {
    "content": "{\"content\":\"{\\\"submission\\\":{\\\"attempt\\\":0,\\\"reply\\\":{\\\"choices\\\":null,\\\"score\\\":\\\"1\\\",\\\"solution\\\":[{\\\"visible\\\":true,\\\"name\\\":\\\"undefined_variable.py\\\",\\\"text\\\":\\\"solution text\\\",\\\"is_visible\\\":true}],\\\"language\\\":null,\\\"code\\\":null,\\\"edu_task\\\":\\\"{\\\\\\\"task\\\\\\\":{\\\\\\\"name\\\\\\\":\\\\\\\"Undefined variable\\\\\\\",\\\\\\\"stepic_id\\\\\\\":186013,\\\\\\\"status\\\\\\\":\\\\\\\"Failed\\\\\\\",\\\\\\\"files\\\\\\\":{\\\\\\\"undefined_variable.py\\\\\\\":{\\\\\\\"name\\\\\\\":\\\\\\\"undefined_variable.py\\\\\\\",\\\\\\\"placeholders\\\\\\\":[{\\\\\\\"offset\\\\\\\":25,\\\\\\\"length\\\\\\\":2,\\\\\\\"dependency\\\\\\\":null,\\\\\\\"hints\\\\\\\":[],\\\\\\\"possible_answer\\\\\\\":\\\\\\\"other_variable\\\\\\\",\\\\\\\"placeholder_text\\\\\\\":\\\\\\\"???\\\\\\\",\\\\\\\"selected\\\\\\\":true}],\\\\\\\"is_visible\\\\\\\":true,\\\\\\\"text\\\\\\\":\\\\\\\"variable = 1\\\\\\\\nprint(???)\\\\\\\\n\\\\\\\"},\\\\\\\"tests.py\\\\\\\":{\\\\\\\"name\\\\\\\":\\\\\\\"tests.py\\\\\\\",\\\\\\\"placeholders\\\\\\\":[],\\\\\\\"is_visible\\\\\\\":false,\\\\\\\"text\\\\\\\":\\\\\\\"from test_helper import test_is_not_empty, test_answer_placeholders_text_deleted, passed, failed, import_task_file\\\\\\\\n\\\\\\\\n\\\\\\\\ndef test_is_identifier():\\\\\\\\n    try:\\\\\\\\n        import_task_file()\\\\\\\\n    except NameError:\\\\\\\\n        passed()\\\\\\\\n        return\\\\\\\\n    except SyntaxError:\\\\\\\\n        failed(\\\\\\\\\\\\\\\"Used invalid identifier\\\\\\\\\\\\\\\")\\\\\\\\n        return\\\\\\\\n    failed(\\\\\\\\\\\\\\\"Use undefined variable\\\\\\\\\\\\\\\")\\\\\\\\n\\\\\\\\n\\\\\\\\nif __name__ == '__main__':\\\\\\\\n    error_text = \\\\\\\\\\\\\\\"You should type undefined variable here\\\\\\\\\\\\\\\"\\\\\\\\n\\\\\\\\n    test_is_not_empty()\\\\\\\\n    test_answer_placeholders_text_deleted(error_text)\\\\\\\\n    test_is_identifier()\\\\\\\\n\\\\\\\"}},\\\\\\\"task_type\\\\\\\":\\\\\\\"edu\\\\\\\"}}\\\",\\\"version\\\":11,\\\"feedback\\\":null},\\\"step\\\":186013,\\\"id\\\":452528473,\\\"status\\\":null,\\\"hint\\\":null,\\\"feedback\\\":null,\\\"time\\\":1628594590068}}\"}"
  }
  """

  @Language("JSON")
  private val descriptors = """
  {
    "descriptors": [
    {
      "id": "b321c43b-46b9-488d-8947-10647795516a",
      "path": "/1/1/000"
    }
    ]
  }
  """
}