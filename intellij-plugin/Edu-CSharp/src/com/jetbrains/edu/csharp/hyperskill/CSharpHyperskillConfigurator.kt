package com.jetbrains.edu.csharp.hyperskill

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.csharp.CSharpConfigurator
import com.jetbrains.edu.csharp.CSharpProjectSettings
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class CSharpHyperskillConfigurator : HyperskillConfigurator<CSharpProjectSettings>(CSharpConfigurator()) {
  override val courseBuilder: EduCourseBuilder<CSharpProjectSettings>
    get() = CSharpHyperskillCourseBuilder()

  override fun shouldFileBeVisibleToStudent(virtualFile: VirtualFile): Boolean =
    virtualFile.name == "Packages" || virtualFile.name == "ProjectSettings" || virtualFile.path.contains("/Assets/")
}
