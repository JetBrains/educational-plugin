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
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotification
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.IOException

class CheckiOCourseUpdater(
  val course: CheckiOCourse,
  val project: Project,
  private val contentGenerator: CheckiOCourseContentGenerator
) {

  @Throws(Exception::class)
  fun doUpdate() {
    val stationsFromServer = contentGenerator.getStationsFromServer()
    val newStations = mutableSetOf<CheckiOStation>()
    val stationsWithNewMissions = mutableSetOf<CheckiOStation>()
    updateStations(stationsFromServer, newStations, stationsWithNewMissions)
    showNewContentUnlockedNotification(newStations, "New stations unlocked")
    showNewContentUnlockedNotification(stationsWithNewMissions, "New missions unlocked in")

    runInEdt {
      synchronize()
      ProjectView.getInstance(project).refresh()
      YamlFormatSynchronizer.saveAll(project)
    }
  }

  private fun showNewContentUnlockedNotification(stations: Collection<CheckiOStation>, title: String) {
    if (stations.isNotEmpty()) {
      Notifications.Bus.notify(CheckiONotification.Info(title, "", stations.joinToString("\n") { it.name }, null))
    }
  }

  private fun updateStations(stationsFromServer: List<CheckiOStation>,
                             newStations: MutableSet<CheckiOStation>,
                             stationsWithNewMissions: MutableSet<CheckiOStation>) {
    val (existingStations, stationsToCreate) = stationsFromServer.partition(course.stations::contains)
    updateExistingStations(existingStations, stationsWithNewMissions)
    course.items = stationsFromServer
    course.init(null, null, false)
    createNewStations(stationsToCreate)
    newStations.addAll(stationsToCreate)
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

  private fun updateExistingStations(stationsToUpdate: List<CheckiOStation>, stationsWithNewMissions: MutableSet<CheckiOStation>) {
    val stationById = course.stations.associateBy { it.id }
    stationsToUpdate.forEach {
      it.course = course
      updateStation(it, stationById[it.id], stationsWithNewMissions)
    }
  }

  private fun updateStation(newStation: CheckiOStation, oldStation: CheckiOStation?, stationsWithNewMissions: MutableSet<CheckiOStation>) {
    if (oldStation == null) {
      return LOG.error("Corresponding local station is not found for station from server [${newStation.id}; ${newStation.name}]")
    }

    var nextNewMissionIndex = oldStation.missions.size + 1

    for (newMission in newStation.missions) {
      val oldMission = oldStation.getMission(newMission.id)
      if (oldMission != null) {
        updateMission(newMission, oldMission)
        newMission.index = oldMission.index
      }
      else {
        createNewMission(oldStation, newStation, newMission, stationsWithNewMissions)
        newMission.index = nextNewMissionIndex
        nextNewMissionIndex++
      }
    }

    newStation.sortItems()
  }

  private fun createNewMission(oldStation: CheckiOStation,
                               newStation: CheckiOStation,
                               newMission: CheckiOMission,
                               stationsWithNewMissions: MutableSet<CheckiOStation>) {
    val lessonDir = oldStation.getLessonDir(project) ?: error("Failed to find station dir: ${oldStation.name}")
    try {
      newMission.lesson = newStation
      GeneratorUtils.createTask(newMission, lessonDir)
      stationsWithNewMissions.add(newStation)
    }
    catch (e: IOException) {
      LOG.error("IO error occurred creating mission [${newMission.id}; ${newMission.name}]", e)
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
        RevertTaskAction.resetDocument(oldDocument, newMission.taskFile)
      }
    } else {
      newMission.taskFile.setText(oldTaskFile.getText())
    }
  }

  companion object {
    private val LOG = Logger.getInstance(CheckiOCourseUpdater::class.java)
  }
}