package com.jetbrains.edu.jbserver

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import org.junit.Test
import org.junit.Assert.assertTrue as check


class UpdateTest : EduTestCase() {

  val service = EduServerApi.create()

  @Test
  fun `test - sample course post get put`() {

    // Post :: initial upload
    println("# POST")
    val courseCC = sampleCourse()
    val metaInfo1 = service.createCourse(courseCC).execute().body()!!
    courseCC.addMetaInformation(metaInfo1)
    println("${courseCC.info()} \n")

    // Get :: get first version
    println("# GET1")
    val courseL1 = service.getCourseMaterials(courseCC.courseId).execute().body()!!
    println("${courseL1.info()} \n")

    // Put :: upload second version
    println("# PUT")
    val part1 = courseCC.items[1] as Section
    val lesson2 = part1.items[0] as Lesson
    val pa5 = lesson2.taskList[1] as OutputTask
    part1.id = 0
    lesson2.id = 0
    pa5.stepId = 0
    pa5.name = "PA #5 new"
    val metaInfo2 = service.updateCourse(courseCC).execute().body()!!
    courseCC.addMetaInformation(metaInfo2)
    println("${courseCC.info()} \n")

    // Get :: get updated version
    println("# GET2")
    val courseL2 = service.getCourseStructure(courseCC.courseId).execute().body()!!
    println("${courseL2.info()} \n")

  }

}
