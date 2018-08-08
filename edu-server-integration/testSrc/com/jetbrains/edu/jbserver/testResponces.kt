package com.jetbrains.edu.jbserver

import java.io.File
import com.fasterxml.jackson.module.kotlin.*


fun main(args: Array<String>) {

  testCase("Deserialize course list") {
    val json = File("/var/www/html/courses").readText()
    val list = mapper.readValue<CourseList>(json)
    list.courses.forEach {
      println("Title:   ${it.name}")
      println("Summary: ${it.description}")
      println()
    }
  }

}
