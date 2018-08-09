package com.jetbrains.edu.learning.checkio

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.EduUtils.synchronize
import com.jetbrains.edu.learning.actions.RefreshTaskFileAction
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import java.io.IOException

abstract class CheckiOCourseUpdater(val course: CheckiOCourse, val project: Project) {
  companion object {
    private val LOG = Logger.getInstance(CheckiOCourseUpdater::class.java)
  }

  protected abstract fun getCourseFromServer() : CheckiOCourse?

  fun doUpdate() {
    val serverCourse = getCourseFromServer()

    if (serverCourse == null) {
      LOG.warn("Couldn't get course from server")
      return
    }

    updateStations(serverCourse)

    runInEdt {
      synchronize()
      ProjectView.getInstance(project).refresh()
    }
  }

  private fun updateStations(serverCourse: CheckiOCourse) {
    val (existingStations, newStations) = serverCourse.stations.partition(course.stations::contains)

    createNewStations(newStations)
    updateExistingStations(existingStations)

    course.items = serverCourse.items
    course.init(null, null, false)
  }

  private fun createNewStations(newStations: List<CheckiOStation>) {
    newStations.forEach{
      it.init(course, course, false)

      try {
        GeneratorUtils.createLesson(it, course.getDir(project))
      } catch (e: IOException) {
        LOG.warn("IO error occurred creating station [${it.id}; ${it.name}]")
        LOG.warn(e.message)
      }
    }
  }

  private fun updateExistingStations(stationsToUpdate: List<CheckiOStation>) {
    val stationById = course.stations.associateBy { it.id }
    stationsToUpdate.forEach {
      updateStation(it, stationById[it.id]!!)
      it.init(course, course, false)
    }
  }

  private fun updateStation(newStation: CheckiOStation, oldStation: CheckiOStation) {
    newStation.missionsList.forEach {
      updateMission(it, oldStation.getMission(it.stepId)!!)
      it.init(course, newStation, false)
    }

    oldStation.missionsList = newStation.missionsList
  }

  private fun updateMission(newMission: CheckiOMission, oldMission: CheckiOMission) {
    val oldTaskFile = oldMission.taskFile

    if (oldTaskFile == null) {
      LOG.warn("TaskFile isn't provided for mission [id=${oldMission.id}; name=${oldMission.name}]")
      return
    }

    val oldVirtualFile = EduUtils.findTaskFileInDir(oldTaskFile, oldMission.getDir(project)!!)

    if (oldVirtualFile == null) {
      LOG.warn("VirtualFile isn't provided for mission [id=${oldMission.id}; name=${oldMission.name}]")
      return
    }

    val secondsFromChangeOnServer = newMission.secondsFromLastChangeOnServer
    val secondsFromLocalChange = (System.currentTimeMillis() - oldVirtualFile.timeStamp) / 1000

    if (secondsFromChangeOnServer < secondsFromLocalChange) {
      oldTaskFile.text = newMission.taskFile?.text // TODO: task file must exist
      newMission.addTaskFile(oldTaskFile)

      val oldDocument = FileDocumentManager.getInstance().getDocument(oldVirtualFile)
      if (oldDocument == null) {
        LOG.warn("Document isn't provided for VirtualFile ${oldVirtualFile.name}")
        return
      }

      ApplicationManager.getApplication().runWriteAction {
        RefreshTaskFileAction.resetDocument(oldDocument, oldTaskFile)
      }
    }
  }
}