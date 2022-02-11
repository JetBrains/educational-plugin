package com.jetbrains.edu.learning.marketplace

import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
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
    val mapper = MarketplaceSubmissionsConnector.getInstance().objectMapper
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
    assertEquals(CheckStatus.Solved.name, submission.status)
    assertEquals("solution text", submission.solutionFiles?.get(0)?.text)
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
    "content": "{\"id\":86515016,\"time\":1644312602091,\"status\":\"Failed\",\"course_version\":4,\"task_id\":234720,\"solution\":[{\"name\":\"src/Task.kt\",\"text\":\"fun start(): String = \\\"NOT OK\\\"\\n\",\"is_visible\":true,\"placeholders\":[{\"offset\":22,\"length\":8,\"dependency\":null,\"possible_answer\":\"\\\"OK\\\"\",\"placeholder_text\":\"TODO()\",\"selected\":true}]}],\"version\":13}"
  }
  """

  //format with a bug, will be fixed on grazie side
  @Language("JSON")
  private val submissionContent = """
  {
    "content": "{\"content\": \"{\\\"id\\\":86515016,\\\"time\\\":1644312602091,\\\"status\\\":\\\"Solved\\\",\\\"course_version\\\":4,\\\"task_id\\\":234720,\\\"solution\\\":[{\\\"name\\\":\\\"src/Task.kt\\\",\\\"text\\\":\\\"solution text\\\",\\\"is_visible\\\":true,\\\"placeholders\\\":[{\\\"offset\\\":22,\\\"length\\\":8,\\\"dependency\\\":null,\\\"possible_answer\\\":\\\"\\\\\\\"OK\\\\\\\"\\\",\\\"placeholder_text\\\":\\\"TODO()\\\",\\\"selected\\\":true}]}],\\\"version\\\":13}\" }"
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