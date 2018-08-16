package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import org.junit.Test
import java.io.File
import org.junit.Assert.assertTrue as check


class ServerTest : EduTestCase() {

  val mapper = jacksonObjectMapper().setupMapper()
  val service = EduServerApi.create()

  val doPost = false

  val testCourse = 4214
  val testSections = listOf(4292, 4365)
  val testLessons = listOf(4366, 4372)
  val testTasks = listOf(4375, 4379)


  @Test
  fun `test - post sample course`() {
    val course = course {
      withName("Sample course")
      withDescription("This is some course description")
      lesson("First lesson") {
        eduTask("PA #1") { }
        eduTask("PA #2") { }
        outputTask("PA #3") { }
      }
      section("Part 1") {
        lesson("Second lesson") {
          outputTask("PA #4") { }
          outputTask("PA #5") { }
        }
        lesson("Third lesson") {
          eduTask("PA #6") { }
          eduTask("PA #7") { }
          eduTask("PA #8") { }
        }
      }
      section("Part 2") {
        lesson("Fourth lesson") {
          outputTask("PA #9") { }
          eduTask("PA #10") { }
          eduTask("PA #11") { }
        }
        lesson("Fifth lesson") {
          outputTask("PA #12") { }
          outputTask("PA #13") { }
          eduTask("PA #14") { }
          eduTask("PA #15") { }
        }
      }
    }.asEduCourse()
    if (doPost) {
      val response = service.createCourse(course).execute()
      val meta = response.body()
      check(meta != null)
      course.addMetaInformation(meta!!)
    }
  }

  @Test
  fun `test - post atomic kotlin course`() {
    val json = File("/var/www/html/atomic_full.json").readText()
    val course = mapper.readValue<EduCourse>(json)
    if (doPost) {
      val response = service.createCourse(course).execute()
      println("Status: ${response.code()} ${response.message()}")
      course.addMetaInformation(response.body()!!)
      println(course.info())
    }

    /* Issue: posting big course ends with timeout.
     *
     * Although course materials are stored on the server,
     * response doesn't come in 15 seconds.
     *
     * Setting read timeout to 30 helps, but that's not normal.
     * Response time: approx 27 sec.
     *
     */
  }

  @Test
  fun `test - get courses list`() {
    val courseList = service.getCourses().execute().body() ?: CourseList()
    courseList.courses.forEach { println(it.info()) }
  }

  @Test
  fun `test - get course materials`() {
    val course = service.getCourseMaterials(testCourse).execute().body()
    println(course!!.info())
  }

  @Test
  fun `test - get courses structure`() {
    val course = service.getCourseStructure(testCourse).execute().body()
    println(course!!.info())
  }

  @Test
  fun `test - get section`() {
    val sections = service.getSections(testSections.pks()).execute().body()
    sections!!.sections.forEach { println(it.info()) }
  }

  @Test
  fun `test - get lessons`() {
    val lessons = service.getLessons(testLessons.pks()).execute().body()
    lessons!!.lessons.forEach { println(it.info()) }
  }

  @Test
  fun `test - get tasks`() {
    val task = service.getTasks(testTasks.pks()).execute().body()
    task!!.tasks.forEach { println(it.info()) }
  }

}
