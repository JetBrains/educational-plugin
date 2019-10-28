package com.jetbrains.edu.learning.checkio

import com.google.common.collect.TreeMultimap
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException
import com.jetbrains.edu.learning.courseFormat.TaskFile

class CheckiOCourseContentGenerator(private val fileType: LanguageFileType, private val apiConnector: CheckiOApiConnector) {
  val stationsFromServer: List<CheckiOStation>
    @Throws(ApiException::class, CheckiOLoginRequiredException::class)
    get() = generateStationsFromMissions(apiConnector.missionList)

  val stationsFromServerUnderProgress: List<CheckiOStation>
    @Throws(Exception::class)
    get() = ProgressManager.getInstance().runProcessWithProgressSynchronously<List<CheckiOStation>, Exception>(
      { stationsFromServer },
      "Getting Course from Server",
      false,
      null
    )

  private fun generateStationsFromMissions(missions: List<CheckiOMission>): List<CheckiOStation> {
    missions.forEach { this.generateTaskFile(it) }

    val stationsMap = TreeMultimap.create(
      Comparator.comparing(CheckiOStation::getId),
      Comparator.comparing(CheckiOMission::getId)
    )

    missions.forEach { mission -> stationsMap.put(mission.station, mission) }

    stationsMap.forEach { station, mission ->
      station.addMission(mission)
      mission.station = station
    }

    return ArrayList(stationsMap.keySet())
  }


  private fun generateTaskFile(mission: CheckiOMission) {
    val taskFile = TaskFile("mission.${fileType.defaultExtension}", mission.code)
    taskFile.isHighlightErrors = true
    mission.addTaskFile(taskFile)
  }
}
