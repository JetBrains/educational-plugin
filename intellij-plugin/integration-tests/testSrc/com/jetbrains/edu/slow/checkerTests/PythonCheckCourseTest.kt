package com.jetbrains.edu.slow.checkerTests

import com.intellij.ide.starter.community.model.BuildType
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.tools.ide.starter.product.pycharm.PyCharm
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory

private const val PYTHON_INTERPRETER_PROPERTY = "project.python.interpreter"

@RequiredProperty(PYTHON_INTERPRETER_PROPERTY)
class PythonBaseCheckCourseTest : CourseValidationTestBase("python", ideInfo = IdeInfo.PyCharm.copy(version = "2026.2.0.1", buildType = BuildType.RELEASE.type)) {

  @TestFactory
  fun `python base test`(): List<DynamicNode> = doTest()
}