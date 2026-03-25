package com.jetbrains.edu.tools.marketplace

import com.jetbrains.edu.learning.courseFormat.EduCourse
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path

const val coursesDir = "/home/iliaposov/Documents/tickets/courses"
const val gradleInfoOutputFile = "/home/iliaposov/programming/educational-plugin-2/marketplace-course-downloader/gradle-intellij.txt"

fun main() {
  val courses = DownloadedCourseArchivesUtil().loadEduCoursesFromDownloadedZips(Path.of(coursesDir))
  val rawOut = ByteArrayOutputStream()
  val out = PrintStream(rawOut, true, "UTF-8")

  for (course in courses) {
    processCourseWithIntellijPlugin(course, out)
  }

  File(gradleInfoOutputFile).writeBytes(rawOut.toByteArray())
}

fun processGradle(course: EduCourse, out: PrintStream) {
  var titleWritten = false

  for (file in course.additionalFiles) {
    if (file.name.contains("gradle")) {
      val contents = file.contents.textualRepresentation
      if (contents.contains("""gradleVersion\s*=\s*\d""".toRegex()) || contents.contains("""distributionUrl=""".toRegex())) {
        if (!titleWritten) {
          out.println()
          out.println("## Course contains files with Gradle version: ${course.id} ${course.name}")
          out.println()
          titleWritten = true
        }

        out.println("----------- <start> ${file.name} -----------")
        out.println(file.contents.textualRepresentation)
        out.println("----------- <end> ${file.name} -----------")
      }
    }
  }
}

fun processGradleWithoutGradleVersion(course: EduCourse, out: PrintStream) {
  val hasGradle = course.additionalFiles.any { it.name.contains("gradle") }
  if (!hasGradle) return
  val hasGradleVersion = course.additionalFiles.any { it.name.contains("gradleVersion") }
  if (hasGradleVersion) return

  out.println("${course.id} ${course.name}")
//  out.println("## Course contains gradle files but without any version: ${course.id} ${course.name}")

  /*for (file in course.additionalFiles) {
    if (file.name.contains("gradle")) {
      out.println("    ${file.name}")
//      val contents = file.contents.textualRepresentation
//      out.println("----------- <start> ${file.name} -----------")
//      out.println(contents)
//      out.println("----------- <end> ${file.name} -----------")
    }
  }*/
}

fun processCourseWithIntellijPlugin(course: EduCourse, out: PrintStream) {
  var titlePrinted = false

  for (file in course.additionalFiles) {
    if (file.name.contains("gradle")) {
      val contents = file.contents.textualRepresentation
      if (contents.contains("intellij", ignoreCase = true)) {
        if (!titlePrinted) {
          out.println("${course.id} ${course.name}")
          titlePrinted = true
        }
        out.println("--- <start> ${file.name} ---")
        out.println(file.contents.textualRepresentation)
        out.println("--- <end> ${file.name} ---")
        return
      }
    }
  }
}

fun mainRenameZipsToHaveUpdateVersionInMane() {
  val pathsToRename = mutableListOf<Pair<Path, Int>>()

  DownloadedCourseArchivesUtil().forEachEduCourseInDownloadedZips(Path.of(coursesDir)) { zipPath, eduCourse ->
    val fileName = zipPath.fileName.toString()
    if (!fileName.matches(Regex(".*_u\\d+\\.zip$"))) {
      eduCourse.marketplaceCourseVersion.let { updateVersion ->
        pathsToRename.add(zipPath to updateVersion)
      }
    }
  }

  pathsToRename.forEach { (oldPath, updateVersion) ->
    val oldFileName = oldPath.fileName.toString()
    val newFileName = oldFileName.replace(Regex("\\.zip$"), "_u$updateVersion.zip")
    val newPath = oldPath.resolveSibling(newFileName)
    Files.move(oldPath, newPath)
    println("Renamed: $oldFileName -> $newFileName")
  }
}