package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.edu.learning.courseFormat.Course
import java.io.File

val service = EduServerApi.create()

fun getCoursesList() {
  val courseList = service.getCourses().execute().body() ?: CourseList()
  courseList.courses.forEach {
    it.print()
    println()
  }
}

fun getCourseMaterials(pk: Int) {
  val course = service.getCourseMaterials(pk).execute().body()
  course!!.print()
}

fun getCourseStructure(pk: Int) {
  val course = service.getCourseStructure(pk).execute().body()
  course!!.print()
}


fun getSections(pks: List<Int>) {
  val sections = service.getSections(pks.pks()).execute().body()
  sections!!.sections.forEach {
    it.print()
  }
}

fun getLessons(pks: List<Int>) {
  val lessons = service.getLessons(pks.pks()).execute().body()
  lessons!!.lessons.forEach {
    it.print()
  }
}

fun getTasks(pks: List<Int>) {
  val task = service.getTasks(pks.pks()).execute().body()
  task!!.tasks.forEach {
    it.print()
  }
}

fun postAtomicKotlin() {
  val json = File("/var/www/html/atomic_full.json").readText()
  val course = mapper.readValue<Course>(json)
  val response = service.createCourse(course).execute()
  println("Status: ${response.code()} ${response.message()}")
}


fun main(args: Array<String>) {

  // getCoursesList()
  // getCourseMaterials()
  // getSections(listOf())
  // getLessons(listOf())
  // getTasks(listOf())
  // postAtomicKotlin()
  // getCourseStructure()

}