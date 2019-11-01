package com.jetbrains.edu.learning.checkio

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.courseFormat.TaskFile

class CheckiOCourseContentGenerator(private val fileType: LanguageFileType, private val apiConnector: CheckiOApiConnector) {
  fun getStationsFromServer(): List<CheckiOStation> {
    val stations = mutableMapOf<Int, CheckiOStation>()

    for (mission in apiConnector.missionList) {
      generateTaskFile(mission)
      val station = stations.computeIfAbsent(mission.station.id) { mission.station }
      station.addMission(mission)
      mission.station = station
    }
    return stations.values.toList()
  }

  fun getStationsFromServerUnderProgress(): List<CheckiOStation> =
    ProgressManager.getInstance().runProcessWithProgressSynchronously<List<CheckiOStation>, Exception>(
      this::getStationsFromServer,
      "Getting Course from Server",
      false,
      null
    )

  private fun generateTaskFile(mission: CheckiOMission) {
    val taskFile = TaskFile("mission.${fileType.defaultExtension}", mission.code)
    taskFile.isHighlightErrors = true
    mission.addTaskFile(taskFile)
  }
}
