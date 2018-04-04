package com.jetbrains.edu.learning

import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.intellij.util.loadElement
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
    expectedFileTree.assertEquals(EduUtils.getCourseDir(project)!!)

    val expected = Paths.get(testDataPath).resolve("${getTestName(true)}.after.xml")
    checkEquals(loadElement(expected), converted)
  }

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
