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

  @Test
  fun `test - get courses list`() {
    val courseList = service.getCourses().execute().body() ?: CourseList()
    courseList.courses.forEach {
      it.print()
      println()
    }
  }

  @Test
  fun `test - get course materials`() {
    val course = service.getCourseMaterials(1520).execute().body()
    course!!.print()
  }

  @Test
  fun `test - get courses structure`() {
    val course = service.getCourseStructure(1520).execute().body()
    course!!.print()
  }

  @Test
  fun `test - get section`() {
    val sections = service.getSections(listOf(1521, 1598).pks()).execute().body()
    sections!!.sections.forEach {
      it.print()
    }
  }

  @Test
  fun `test - get lessons`() {
    val lessons = service.getLessons(listOf(1528, 1534).pks()).execute().body()
    lessons!!.lessons.forEach {
      it.print()
    }
  }

  @Test
  fun `test - get tasks`() {
    val task = service.getTasks(listOf(1532, 1533).pks()).execute().body()
    task!!.tasks.forEach {
      it.print()
    }
  }

  @Test
  fun `test - post atomic kotlin course`() {
    val json = File("/var/www/html/atomic_full.json").readText()
    val course = mapper.readValue<EduCourse>(json)
    if (doPost) {
      val response = service.createCourse(course).execute()
      println("Status: ${response.code()} ${response.message()}")
      response.body()?.print()
    }
  }

  @Test
  fun `test - merge course meta info`() {
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
    }.asEduCourse()

    val response = service.createCourse(course).execute()
    val meta = response.body()
    check(meta != null)

    course.addMetaInformation(meta!!)
  }

}
