package com.jetbrains.edu.coursecreator.checkio

import com.google.gson.reflect.TypeToken
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.api.CheckiOApiInterface
import com.jetbrains.edu.learning.checkio.api.RetrofitUtils.createApiGson
import com.jetbrains.edu.learning.checkio.call.CheckiOCall
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.io.File

class CheckiOCourseGenerationTest : EduTestCase() {
  private val missionsJson = FileUtil.loadFile(File(testDataPath, "missions.json"))

  fun `test deserialized missions list has correct size`() {
    assertEquals(6, getSourceMissions().size)
  }

  fun `test order of missions`() {
    val sourceMissions = getSourceMissions()
    val processedMissions = getProcessedMissions()
    assertEquals(sourceMissions.size, processedMissions.size)

    for ((source, processed) in sourceMissions.zip(processedMissions)) {
      assertEquals(source.name, processed.name)
    }
  }

  fun `test links to task and solutions generated correctly`() {
    val missions = getProcessedMissions()

    val solved = missions.first { it.status == CheckStatus.Solved }
    val expectedDescription = """
      <p><a href="https://py.checkio.org/mission/stressful-subject/publications/category/clear">View solutions</a></p>
      <p><a href="https://py.checkio.org/en/mission/stressful-subject">See the task on CheckiO</a></p>
    """.trimIndent()
    assertEquals(expectedDescription, solved.descriptionText)

    // We should not have link to other solutions if the task was not solved
    val notSolved = missions.filter { it.status != CheckStatus.Solved && it.descriptionText.contains("View other solutions") }
    assertEmpty(notSolved)
  }

  private fun getSourceMissions() = MockCheckiOApiConnector().missionList

  private fun getProcessedMissions(): List<CheckiOMission> {
    val stations = CheckiOCourseContentGenerator(PlainTextFileType.INSTANCE, MockCheckiOApiConnector()).getStationsFromServer()
    return stations.flatMap { it.missions }
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/checkio"
  }

  inner class MockCheckiOApiConnector : CheckiOApiConnector(MockCheckiOApiInterface(), MockCheckiOOAuthConnector()) {
    override fun getMissionList(): List<CheckiOMission> {
      return createApiGson().fromJson(missionsJson, object : TypeToken<MutableList<CheckiOMission>>() {}.type)
    }

    override fun getLanguageId()= "py"
  }

  private class MockCheckiOApiInterface : CheckiOApiInterface {
    override fun getMissionList(accessToken: String?): CheckiOCall<MutableList<CheckiOMission>> = error("unreachable code")
  }

  private class MockCheckiOOAuthConnector : CheckiOOAuthConnector("", "") {
    override fun setAccount(account: CheckiOAccount?) {}

    override fun getOAuthServicePath(): String = ""

    override fun getPlatformName(): String = ""

    override fun getAccount(): CheckiOAccount? = null
  }
}