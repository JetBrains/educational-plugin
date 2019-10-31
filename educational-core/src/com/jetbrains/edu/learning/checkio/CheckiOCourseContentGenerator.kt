package com.jetbrains.edu.learning.checkio

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.courseFormat.TaskFile

class CheckiOCourseContentGenerator(private val fileType: LanguageFileType, private val apiConnector: CheckiOApiConnector) {
  val stationsFromServer: List<CheckiOStation>
    get() = generateStationsFromMissions(apiConnector.missionList)

  val stationsFromServerUnderProgress: List<CheckiOStation>
    get() = ProgressManager.getInstance().runProcessWithProgressSynchronously<List<CheckiOStation>, Exception>(
      { stationsFromServer },
      "Getting Course from Server",
      false,
      null
    )

  private fun generateStationsFromMissions(missions: List<CheckiOMission>): List<CheckiOStation> {
    val stations = missions.map { it.station.id to it.station }.distinctBy { it.first }.toMap()

    for (mission in missions) {
      generateTaskFile(mission)
      val station = stations[mission.station.id] ?: continue
      station.addMission(mission)
      mission.station = station
    }

    return stations.values.toList()
  }

  private fun generateTaskFile(mission: CheckiOMission) {
    val taskFile = TaskFile("mission.${fileType.defaultExtension}", mission.code)
    taskFile.isHighlightErrors = true
    mission.addTaskFile(taskFile)
  }
}
