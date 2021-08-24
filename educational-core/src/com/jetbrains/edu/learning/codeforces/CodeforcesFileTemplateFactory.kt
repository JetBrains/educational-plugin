package com.jetbrains.edu.learning.codeforces

import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TITLE

class CodeforcesFileTemplateFactory : FileTemplateGroupDescriptorFactory {

  override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
    val group = FileTemplateGroupDescriptor(CODEFORCES_TITLE, EducationalCoreIcons.Codeforces)
    CodeforcesLanguageProvider.EP_NAME.extensions.forEach {
      group.addTemplate(CodeforcesFileTemplateDescriptor(it.templateFileName, it.languageIcon, it.displayTemplateName))
    }
    return group
  }

}