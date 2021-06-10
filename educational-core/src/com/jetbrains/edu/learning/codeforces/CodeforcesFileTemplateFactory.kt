package com.jetbrains.edu.learning.codeforces

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory
import com.jetbrains.edu.learning.EduNames
import icons.EducationalCoreIcons

class CodeforcesFileTemplateFactory : FileTemplateGroupDescriptorFactory {

  override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
    val group = FileTemplateGroupDescriptor("Codeforces", EducationalCoreIcons.Codeforces)
    group.addTemplate(FileTemplateDescriptor(EduNames.CODEFORCES_CPP_TEMPLATE, EducationalCoreIcons.CppLogo))
    group.addTemplate(FileTemplateDescriptor(EduNames.CODEFORCES_GO_TEMPLATE, EducationalCoreIcons.GoLogo))
    group.addTemplate(FileTemplateDescriptor(EduNames.CODEFORCES_JAVA_TEMPLATE, EducationalCoreIcons.JavaLogo))
    group.addTemplate(FileTemplateDescriptor(EduNames.CODEFORCES_JS_TEMPLATE, EducationalCoreIcons.JsLogo))
    group.addTemplate(FileTemplateDescriptor(EduNames.CODEFORCES_KOTLIN_TEMPLATE, EducationalCoreIcons.KotlinLogo))
    group.addTemplate(FileTemplateDescriptor(EduNames.CODEFORCES_PYTHON_TEMPLATE, EducationalCoreIcons.PythonLogo))
    group.addTemplate(FileTemplateDescriptor(EduNames.CODEFORCES_RUST_TEMPLATE, EducationalCoreIcons.RustLogo))
    group.addTemplate(FileTemplateDescriptor(EduNames.CODEFORCES_SCALA_TEMPLATE, EducationalCoreIcons.ScalaLogo))
    return group
  }

}