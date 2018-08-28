package com.jetbrains.edu.learning.checkio

import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.Notifications
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.EduUtils.synchronize
import com.jetbrains.edu.learning.actions.RefreshTaskFileAction
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.checkio.notifications.infos.CheckiOStationsUnlockedNotification
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import java.io.IOException

class CheckiOCourseUpdater(
  val course: CheckiOCourse,
  val project: Project,
  private val contentGenerator: CheckiOCourseContentGenerator
) {

  @Throws(Exception::class)
  fun doUpdate() {
    val stationsFromServer = contentGenerator.stationsFromServer
    val newStations = updateStations(stationsFromServer)
    showNotification(newStations)

    runInEdt {
      synchronize()
      ProjectView.getInstance(project).refresh()
    }
  }

  private fun showNotification(newStations: List<CheckiOStation>) {
    if (newStations.isNotEmpty()) {
      Notifications.Bus.notify(CheckiOStationsUnlockedNotification(newStations))
    }
  }

  private fun updateStations(stationsFromServer: List<CheckiOStation>): List<CheckiOStation> {
    val (existingStations, newStations) = stationsFromServer.partition(course.stations::contains)

    updateExistingStations(existingStations)
    course.items = stationsFromServer
    course.init(null, null, false)
    createNewStations(newStations)

    return newStations
  }

  private fun createNewStations(newStations: List<CheckiOStation>) {
    newStations.forEach {
      try {
        GeneratorUtils.createLesson(it, course.getDir(project))
      }
      catch (e: IOException) {
        LOG.error("IO error occurred creating station [${it.id}; ${it.name}]", e)
      }
    }
  }

  private fun updateExistingStations(stationsToUpdate: List<CheckiOStation>) {
    val stationById = course.stations.associateBy { it.id }
    stationsToUpdate.forEach {
      updateStation(it, stationById[it.id])
    }
  }

  private fun updateStation(newStation: CheckiOStation, oldStation: CheckiOStation?) {
    if (oldStation == null) {
      return LOG.error("Corresponding local station is not found for station from server [${newStation.id}; ${newStation.name}]")
    }

    newStation.missions.forEach {
      updateMission(it, oldStation.getMission(it.id))
    }
  }

  private fun updateMission(newMission: CheckiOMission, oldMission: CheckiOMission?) {
    if (oldMission == null) {
      return LOG.error("Corresponding local mission is not found for mission from server [${newMission.id}; ${newMission.name}]")
    }

    val oldTaskFile = oldMission.taskFile

    val oldMissionDir = oldMission.getDir(project)
                        ?: return LOG.error("Directory is not found for mission [${oldMission.id}; ${oldMission.name}]")

    val oldVirtualFile = EduUtils.findTaskFileInDir(oldTaskFile, oldMissionDir)
                         ?: return LOG.error("VirtualFile is not found for mission [id=${oldMission.id}; name=${oldMission.name}]")

    val secondsFromChangeOnServer = newMission.secondsFromLastChangeOnServer
    val secondsFromLocalChange = (System.currentTimeMillis() - oldVirtualFile.timeStamp) / 1000

    if (secondsFromChangeOnServer < secondsFromLocalChange) {
      val oldDocument = runReadAction {
        FileDocumentManager.getInstance().getDocument(oldVirtualFile)
      }

      if (oldDocument == null) {
        return LOG.error("Document isn't provided for VirtualFile ${oldVirtualFile.name}")
      }

      runWriteAction {
        RefreshTaskFileAction.resetDocument(oldDocument, newMission.taskFile)
      }
    } else {
      newMission.taskFile.setText(oldTaskFile.getText())
    }
  }

  companion object {
    private val LOG = Logger.getInstance(CheckiOCourseUpdater::class.java)
  }
}