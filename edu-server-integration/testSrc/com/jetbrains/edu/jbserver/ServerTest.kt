package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduCourse
import org.junit.Test
import java.io.File
import org.junit.Assert.assertTrue as check


class ServerTest : EduTestCase() {

  val mapper = jacksonObjectMapper().setupMapper()
  val service = EduServerApi.create()

  val doPost = false

  val testCourse = 5505
  val testSections = listOf(5510, 5518)
  val testLessons = listOf(5506, 5519)
  val testTasks = listOf(5513, 5525)


  @Test
  fun `test - post sample course`() {
    val course = sampleCourse()
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
