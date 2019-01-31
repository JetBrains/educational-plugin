package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.stepik.api.StepikConnector.service
import com.jetbrains.edu.learning.stepik.checkForErrors
import com.jetbrains.edu.learning.stepik.executeHandlingExceptions

object StepikMultipleRequestsConnector {
  private const val MAX_REQUEST_PARAMS = 100 // restriction of Stepik API for multiple requests

  fun getUsers(result: List<EduCourse>): MutableList<StepikUserInfo> {
    val instructorIds = result.flatMap { it.instructors }.distinct().chunked(MAX_REQUEST_PARAMS)
    val allUsers = mutableListOf<StepikUserInfo>()
    instructorIds
      .mapNotNull {
        val response = service.users(*it.toIntArray()).executeHandlingExceptions()
        checkForErrors(response)
        response?.body()?.users
      }
      .forEach { allUsers.addAll(it) }
    return allUsers
  }

  fun getSections(sectionIds: List<Int>): List<Section> {
    val sectionIdsChunks = sectionIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allSections = mutableListOf<Section>()
    sectionIdsChunks
      .mapNotNull {
        val response = service.sections(*it.toIntArray()).executeHandlingExceptions()
        checkForErrors(response)
        response?.body()?.sections
      }
      .forEach { allSections.addAll(it) }
    return allSections
  }

  fun getLessons(lessonIds: List<Int>): List<Lesson> {
    val lessonsIdsChunks = lessonIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allLessons = mutableListOf<Lesson>()
    lessonsIdsChunks
      .mapNotNull {
        val response = service.lessons(*it.toIntArray()).executeHandlingExceptions()
        checkForErrors(response)
        response?.body()?.lessons
      }
      .forEach { allLessons.addAll(it) }
    return allLessons
  }

  fun getUnits(unitIds: List<Int>): List<StepikUnit> {
    val unitsIdsChunks = unitIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allUnits = mutableListOf<StepikUnit>()
    unitsIdsChunks
      .mapNotNull {
        val response = service.units(*it.toIntArray()).executeHandlingExceptions()
        checkForErrors(response)
        response?.body()?.units
      }
      .forEach { allUnits.addAll(it) }
    return allUnits
  }

  fun getAssignments(ids: List<Int>): List<Assignment> {
    val idsChunks = ids.distinct().chunked(MAX_REQUEST_PARAMS)
    val assignments = mutableListOf<Assignment>()
    idsChunks
      .mapNotNull {
        val response = service.assignments(*it.toIntArray()).executeHandlingExceptions()
        checkForErrors(response)
        response?.body()?.assignments
      }
      .forEach { assignments.addAll(it) }

    return assignments
  }

  fun getStepSources(stepIds: List<Int>): List<StepSource> {
    val stepsIdsChunks = stepIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val steps = mutableListOf<StepSource>()
    stepsIdsChunks
      .mapNotNull {
        val response = service.steps(*it.toIntArray()).executeHandlingExceptions()
        checkForErrors(response)
        response?.body()?.steps
      }
      .forEach { steps.addAll(it) }
    return steps
  }

  fun taskStatuses(ids: List<String>): List<Boolean>? {
    val idsChunks = ids.distinct().chunked(MAX_REQUEST_PARAMS)
    val progresses = mutableListOf<Progress>()
    idsChunks
      .mapNotNull {
        val response = service.progresses(*it.toTypedArray()).executeHandlingExceptions()
        checkForErrors(response)
        response?.body()?.progresses
      }
      .forEach { progresses.addAll(it) }

    val progressesMap = progresses.associate { it.id to it.isPassed }
    return ids.mapNotNull { progressesMap[it] }
  }
}
