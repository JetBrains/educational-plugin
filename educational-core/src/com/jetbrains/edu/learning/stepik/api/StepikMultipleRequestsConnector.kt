package com.jetbrains.edu.learning.stepik.api

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.stepik.api.StepikConnector.service

object StepikMultipleRequestsConnector {
  private const val MAX_REQUEST_PARAMS = 100 // restriction of Stepik API for multiple requests
  private val LOG = Logger.getInstance(StepikMultipleRequestsConnector::class.java)

  fun getUsers(result: List<EduCourse>): MutableList<StepikUserInfo> {
    val instructorIds = result.flatMap { it -> it.instructors }.distinct().chunked(MAX_REQUEST_PARAMS)
    val allUsers = mutableListOf<StepikUserInfo>()
    instructorIds
      .mapNotNull { service.users(*it.toIntArray()).execute().body()?.users }
      .forEach { allUsers.addAll(it) }
    return allUsers
  }

  fun getSections(sectionIds: List<Int>): List<Section> {
    val sectionIdsChunks = sectionIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allSections = mutableListOf<Section>()
    sectionIdsChunks
      .mapNotNull { service.sections(*it.toIntArray()).execute().body()?.sections }
      .forEach { allSections.addAll(it) }
    return allSections
  }

  fun getLessons(lessonIds: List<Int>): List<Lesson> {
    val lessonsIdsChunks = lessonIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allLessons = mutableListOf<Lesson>()
    lessonsIdsChunks
      .mapNotNull { service.lessons(*it.toIntArray()).execute().body()?.lessons }
      .forEach { allLessons.addAll(it) }
    return allLessons
  }

  fun getUnits(unitIds: List<Int>): List<StepikUnit> {
    val unitsIdsChunks = unitIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allUnits = mutableListOf<StepikUnit>()
    unitsIdsChunks
      .mapNotNull { service.units(*it.toIntArray()).execute().body()?.units }
      .forEach { allUnits.addAll(it) }
    return allUnits
  }

  fun getAssignments(ids: List<Int>): List<Assignment> {
    val idsChunks = ids.distinct().chunked(MAX_REQUEST_PARAMS)
    val assignments = mutableListOf<Assignment>()
    idsChunks
      .mapNotNull { service.assignments(*it.toIntArray()).execute().body()?.assignments }
      .forEach { assignments.addAll(it) }

    return assignments
  }

  fun getStepSources(stepIds: List<Int>, language: String): List<StepSource> {
    // TODO: use language parameter
    val stepsIdsChunks = stepIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val steps = mutableListOf<StepSource>()
    stepsIdsChunks
      .mapNotNull { service.steps(*it.toIntArray()).execute().body()?.steps }
      .forEach { steps.addAll(it) }
    return steps
  }

  fun taskStatuses(ids: List<String>): List<Boolean>? {
    val idsChunks = ids.distinct().chunked(MAX_REQUEST_PARAMS)
    val progresses = mutableListOf<Progress>()
    idsChunks
      .mapNotNull { service.progresses(*it.toTypedArray()).execute().body()?.progresses }
      .forEach { progresses.addAll(it) }

    val progressesMap = progresses.associate { it.id to it.isPassed }
    return ids.mapNotNull { progressesMap[it] }
  }
}
