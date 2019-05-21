package com.jetbrains.edu.learning.format

import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.intellij.util.loadElement
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.serialization.SerializationUtils
import junit.framework.ComparisonFailure
import org.jdom.Element
import java.nio.file.Paths

class StudyMigrationTest : LightPlatformCodeInsightFixtureTestCase() {

  fun testFromThirdToForth() = doTest(3)

  fun testAdaptive45() = doTest(4)

  fun testSubtasks45() = doTest(4)

  fun testTheory35To4() = doTest(4)

  fun testTheory351To4() = doTest(4)

  fun testPycharmToEdu() = doTest(7)

  fun testFromEighthToNinth() {
    myFixture.copyDirectoryToProject("toNinth/lesson1", "lesson1")

    val element = loadElement(Paths.get(testDataPath).resolve("${getTestName(true)}.xml"))
    val converted = SerializationUtils.Xml.convertToNinthVersion(project, element)

    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("First task") {
          file("task.html")
          file("task.txt")
        }
        dir("First task (1)") {
          file("task.html")
          file("task.txt")
        }
      }
    }
    expectedFileTree.assertEquals(project.courseDir)

    val expected = Paths.get(testDataPath).resolve("${getTestName(true)}.after.xml")
    checkEquals(loadElement(expected), converted)
  }

  fun test9to10() = doTest(9)
  fun test10to11kotlin() = doTest(10)
  fun test10to11python() = doTest(10)
  fun test11to12Stepik() = doTest(11)
  fun test11to12Remote() = doTest(11)
  fun test11to12Coursera() = doTest(11)
  fun test11to12CheckiO() = doTest(11)
  fun test11to12Local() = doTest(11)
  fun test12to13() = doTest(12)
  fun test13to14() = doTest(13)
  fun test13to14Android() = doTest(13)
  fun test14to15() = doTest(14)

  private fun doTest(version: Int) {
    val name = getTestName(true)
    val before = Paths.get(testDataPath).resolve("$name.xml")
    val after = Paths.get(testDataPath).resolve("$name.after.xml")
    val element = loadElement(before)
    var converted = element
    when (version) {
      1 -> converted = SerializationUtils.Xml.convertToSecondVersion(project, element)
      3 -> converted = SerializationUtils.Xml.convertToFourthVersion(project, element)
      4 -> converted = SerializationUtils.Xml.convertToFifthVersion(project, element)
      7 -> converted = SerializationUtils.Xml.convertToSeventhVersion(project, element)
      9 -> converted = SerializationUtils.Xml.convertToTenthVersion(project, element)
      10 -> converted = SerializationUtils.Xml.convertToEleventhVersion(project, element)
      11 -> converted = SerializationUtils.Xml.convertTo12Version(project, element)
      12 -> converted = SerializationUtils.Xml.convertTo13Version(project, element)
      13 -> converted = SerializationUtils.Xml.convertTo14Version(project, element)
      14 -> converted = SerializationUtils.Xml.convertTo15Version(project, element)
    }
    checkEquals(loadElement(after), converted)
  }

  private fun checkEquals(expected: Element, actual: Element) {
    if (!JDOMUtil.areElementsEqual(expected, actual)) {
      throw ComparisonFailure("Elements are not equal", JDOMUtil.writeElement(expected), JDOMUtil.writeElement(actual))
    }
  }

  override fun getTestDataPath() = "testData/migration"

}
