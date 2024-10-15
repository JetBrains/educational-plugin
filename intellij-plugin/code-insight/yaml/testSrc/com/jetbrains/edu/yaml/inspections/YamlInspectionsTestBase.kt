package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.edu.yaml.YamlCodeInsightTest
import kotlin.reflect.KClass

abstract class YamlInspectionsTestBase(private val inspectionClass: KClass<out LocalInspectionTool>) : YamlCodeInsightTest() {

  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(listOf(inspectionClass.java))
  }

  protected fun testQuickFix(item: StudyItem, fixName: String, configTextBefore: String, configTextAfter: String) {
    testHighlighting(item, configTextBefore)
    val action = myFixture.findSingleIntention(fixName)
    myFixture.launchAction(action)
    val configFile = item.getDir(project.courseDir)!!.findChild(item.configFileName)!!
    myFixture.openFileInEditor(configFile)

    val configTextWithoutHighlighting = configTextAfter.replace(Regex("<.*?>"), "")
    myFixture.checkResult(configTextWithoutHighlighting)
    testHighlighting(item, configTextAfter)
  }

  protected fun testQuickFixIsUnavailable(item: StudyItem, fixName: String, text: String) {
    testHighlighting(item, text)
    check(myFixture.filterAvailableIntentions(fixName).isEmpty()) {
      "Fix `$fixName` should not be possible to apply."
    }
  }

  protected fun testHighlighting(item: StudyItem, configText: String) {
    // BACKCOMPAT: 2024.2 remove the fixedConfig variable completely and use configText instead.
    val fixedConfig = if (ApplicationInfo.getInstance().build < BuildNumber.fromString("243")!!) {
      configText.replace("wrong_property</warning>: prop", "wrong_property: prop</warning>")
    }
    else {
      configText
    }

    openConfigFileWithText(item, fixedConfig)
    myFixture.checkHighlighting()
  }
}
