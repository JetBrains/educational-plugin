package com.jetbrains.edu.jbserver

import java.io.File
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


val resPath = "edu-server-integration/testResources"


val mapper = jacksonObjectMapper().setupMapper()


fun testCase(name: String, test: () -> Unit) {
  try {
    test()
  } catch (e: IllegalStateException) {
    println("Test `$name` failed (check).")
  } catch (e: Exception) {
    println("Test `$name` failed (exception):")
    e.printStackTrace()
  }
}


fun jsonEquals(json1: String, json2: String): Boolean {
  val compareMapper = jacksonObjectMapper()
  val tree1 = compareMapper.readTree(json1)
  val tree2 = compareMapper.readTree(json2)
  return tree1 == tree2
}


fun readResFile(filename: String) =
  File("$resPath/$filename").readText()


fun main(args: Array<String>) {

  testDeserializationCourse()
  testDeserializationTaskFile()
  testDeserializationTask()

  testSerializationTaskFile()
  testSerializationTask()
  testSerializationCourse()

}
