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
import com.jetbrains.edu.learning.messages.EduCoreBundle
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
    val actualDescriptionText = solved.descriptionText

    assertTrue(actualDescriptionText.contains("<h2>Stressful Subject</h2>"))
    assertTrue(actualDescriptionText.contains(EduCoreBundle.message("checkio.view.solutions")))
    assertTrue(actualDescriptionText.contains(EduCoreBundle.message("checkio.open.task.on.site")))

    // We should not have link to other solutions if the task was not solved
    val notSolved = missions.filter { it.status != CheckStatus.Solved && it.descriptionText.contains("View other solutions") }
    assertEmpty(notSolved)
  }

  @Suppress("deprecation")
  fun `test station and mission names are computed correctly`() {
    // `:` was taken because it is forbidden both on Unix & Windows platforms
    val missions = getSourceMissions()
    missions.forEach {
      assertFalse(it.name.contains(':'))
      assertFalse(it.station.name.contains(':'))
    }

    // Check if we set custom name for hard names
    val hardNamesmission = missions.first { it.id == 520 }
    assertNotNull(hardNamesmission.customPresentableName)
    assertNotNull(hardNamesmission.station.customPresentableName)

    // Check if we do not set custom name for simple names
    val simpleNamesMission = missions.first { it.id == 566 }
    assertNull(simpleNamesMission.customPresentableName)
    assertNull(simpleNamesMission.station.customPresentableName)
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
    override var account: CheckiOAccount? = null

    override val oAuthServicePath: String = ""

    override val platformName: String = ""
  }
}