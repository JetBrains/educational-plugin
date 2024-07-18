package com.jetbrains.edu.learning.checkio

import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.CourseUpdateListener
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOCourse
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOStation
import com.jetbrains.edu.learning.courseFormat.ext.findTaskFileInDir
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
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
    showNewContentUnlockedNotification(newStations, EduCoreBundle.message("notification.title.new.station.unlocked"))
    showNewContentUnlockedNotification(stationsWithNewMissions, EduCoreBundle.message("notification.title.new.missions.unlocked.in"))

    runInEdt {
      EduUtilsKt.synchronize()
      ProjectView.getInstance(project).refresh()
      YamlFormatSynchronizer.saveAll(project)
      EduUtilsKt.updateToolWindows(project)
      project.messageBus.syncPublisher(CourseUpdateListener.COURSE_UPDATE).courseUpdated(project, course)
    }
  }

  private fun showNewContentUnlockedNotification(
    stations: Collection<CheckiOStation>,
    @NotificationTitle title: String
  ) {
    if (stations.isNotEmpty()) {
      EduNotificationManager
        .create(INFORMATION, title, stations.joinToString("\n") { it.name })
        .setIcon(EducationalCoreIcons.Platform.Tab.CheckiOTab)
        .notify(null)
    }
  }

  private fun updateStations(stationsFromServer: List<CheckiOStation>,
                             newStations: MutableSet<CheckiOStation>,
                             stationsWithNewMissions: MutableSet<CheckiOStation>) {
    val (existingStations, stationsToCreate) = stationsFromServer.partition(course.stations::contains)
    updateExistingStations(existingStations, stationsWithNewMissions)
    course.items = stationsFromServer
    course.init(false)
    createNewStations(stationsToCreate)
    newStations.addAll(stationsToCreate)
  }

  private fun createNewStations(newStations: List<CheckiOStation>) {
    newStations.forEach {
      try {
        GeneratorUtils.createLesson(project, it, project.courseDir)
      }
      catch (e: IOException) {
        LOG.error("IO error occurred creating station [${it.id}; ${it.name}]", e)
      }
    }
  }

  private fun updateExistingStations(stationsToUpdate: List<CheckiOStation>, stationsWithNewMissions: MutableSet<CheckiOStation>) {
    val stationById = course.stations.associateBy { it.id }
    stationsToUpdate.forEach {
      it.parent = course
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
    val lessonDir = oldStation.getDir(project.courseDir) ?: error("Failed to find station dir: ${oldStation.name}")
    try {
      newMission.parent = newStation
      GeneratorUtils.createTask(project, newMission, lessonDir)
      stationsWithNewMissions.add(newStation)
    }
    catch (e: IOException) {
      LOG.error("IO error occurred creating mission [${newMission.id}; ${newMission.name}]", e)
    }
  }

  private fun updateMission(newMission: CheckiOMission, oldMission: CheckiOMission) {
    val oldTaskFile = oldMission.getTaskFile()

    val oldMissionDir = oldMission.getDir(project.courseDir)
                        ?: return LOG.error("Directory is not found for mission [${oldMission.id}; ${oldMission.name}]")

    val oldVirtualFile = oldTaskFile.findTaskFileInDir(oldMissionDir)
                         ?: return LOG.error("VirtualFile is not found for mission [id=${oldMission.id}; name=${oldMission.name}]")

    val secondsFromChangeOnServer = newMission.secondsFromLastChangeOnServer
    val secondsFromLocalChange = (System.currentTimeMillis() - oldVirtualFile.timeStamp) / 1000

    if (secondsFromChangeOnServer < secondsFromLocalChange) {
      val oldDocument = runReadAction {
        FileDocumentManager.getInstance().getDocument(oldVirtualFile)
      } ?: return LOG.error("Document isn't provided for VirtualFile ${oldVirtualFile.name}")

      runWriteAction {
        newMission.getTaskFile().apply {
          isTrackChanges = false
          oldDocument.setText(text)
          isTrackChanges = true
        }
      }

      val descriptionFile = oldMission.getDescriptionFile(project) ?: return
      val descriptionDocument = FileDocumentManager.getInstance().getDocument(descriptionFile) ?: return

      runWriteAction {
        descriptionDocument.setText(newMission.descriptionText)
      }
    } else {
      newMission.getTaskFile().text = oldTaskFile.text
    }
  }

  companion object {
    private val LOG = Logger.getInstance(CheckiOCourseUpdater::class.java)
  }
}