package com.jetbrains.edu.jbserver


fun main(args: Array<String>) {

  val service = EduServerApi.create()
  val courseList = service.getCourses().execute().body() ?: CourseList()

  courseList.courses.asSequence().forEach {
    println("Name: ${it.name}")
    println("Desc: ${it.description}")
    println()
  }

}
