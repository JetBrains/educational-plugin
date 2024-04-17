package com.jetbrains.edu.coursecreator.checkio

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOCourse
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.junit.Test

class CheckiOCourseGenerationTest : EduTestCase() {
  private val contentGenerator
    get() = CheckiOCourseContentGenerator(PlainTextFileType.INSTANCE, MockCheckiOApiConnector)

  private val mockConnector: MockCheckiOApiConnector
    get() = MockCheckiOApiConnector

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { _, _ ->
      MockResponseFactory.fromFile(getTestFile("missions.json"))
    }
  }

  @Test
  fun `test deserialized missions list has correct size`() {
    assertEquals(6, getSourceMissions().size)
  }

  @Test
  fun `test order of missions`() {
    val sourceMissions = getSourceMissions()
    val processedMissions = getProcessedMissions()
    assertEquals(sourceMissions.size, processedMissions.size)

    for ((source, processed) in sourceMissions.zip(processedMissions)) {
      assertEquals(source.name, processed.name)
    }
  }

  @Test
  fun `test links to task and solutions generated correctly`() {
    val missions = getProcessedMissions()

    val solved = missions.first { it.status == CheckStatus.Solved }
    val actualDescriptionText = solved.descriptionText

    assertFalse(actualDescriptionText.contains("<h2>Stressful Subject</h2>"))
    assertTrue(actualDescriptionText.contains(EduCoreBundle.message("checkio.view.solutions")))
    assertTrue(actualDescriptionText.contains(EduCoreBundle.message("checkio.open.task.on.site")))

    // We should not have link to other solutions if the task was not solved
    val notSolved = missions.filter { it.status != CheckStatus.Solved && it.descriptionText.contains("View other solutions") }
    assertEmpty(notSolved)
  }

  @Test
  fun `test station and mission names are computed correctly`() {
    val checkiOCourse = course(courseProducer = ::CheckiOCourse) {} as CheckiOCourse
    val stations = contentGenerator.getStationsFromServer()
    stations.forEach { checkiOCourse.addStation(it) }
    checkiOCourse.createCourseFiles(project)

    fun FileTreeBuilder.dirWithDefaultFiles(dirName: String) {
      dir(dirName) {
        file("task.html")
        file("mission.txt")
      }
    }

    checkFileTree {
      dir("Home") {
        dirWithDefaultFiles("All the Same")
        dirWithDefaultFiles("The Warriors")
      }
      dir("SendGrid") {
        dirWithDefaultFiles("Stressful Subject")
      }
      dir("Elementary") {
        dirWithDefaultFiles("Multiply (Intro)")
        dirWithDefaultFiles("Say Hi")
        dirWithDefaultFiles("Easy Unpack")
      }
    }
  }

  private fun getSourceMissions() = MockCheckiOApiConnector.getMissionList()

  private fun getProcessedMissions(): List<CheckiOMission> {
    val stations = contentGenerator.getStationsFromServer()
    return stations.flatMap { it.missions }
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/checkio/"
  }
}