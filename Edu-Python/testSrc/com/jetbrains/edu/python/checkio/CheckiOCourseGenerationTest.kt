package com.jetbrains.edu.python.checkio

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.api.CheckiOApiInterface
import com.jetbrains.edu.learning.checkio.api.adapters.CheckiOMissionListDeserializer
import com.jetbrains.edu.learning.checkio.call.CheckiOCall
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.python.PythonFileType
import java.io.File

class CheckiOCourseGenerationTest : EduTestCase() {
  private val missionsJson = FileUtil.loadFile(File(testDataPath, "missions.json"))

  fun `test order of missions`() {
    val sourceMissions = MockCheckiOApiConnector().missionList

    val processedStations = CheckiOCourseContentGenerator(PythonFileType.INSTANCE, MockCheckiOApiConnector()).stationsFromServer
    val processedMissions = processedStations.flatMap { it.missions }

    assertTrue(processedMissions.zip(sourceMissions).all { it.first.name == it.second.name })
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/checkio"
  }

  inner class MockCheckiOApiConnector : CheckiOApiConnector(MockCheckiOApiInterface(), MockCheckiOOAuthConnector()) {
    override fun getMissionList(): List<CheckiOMission> {
      val gson = GsonBuilder().registerTypeAdapter(object : TypeToken<List<CheckiOMission>>() {}.type,
                                                   CheckiOMissionListDeserializer()).create()
      return gson.fromJson(missionsJson, object : TypeToken<List<CheckiOMission>>() {}.type)
    }
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