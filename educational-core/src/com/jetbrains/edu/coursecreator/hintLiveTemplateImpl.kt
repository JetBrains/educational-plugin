package com.jetbrains.edu.coursecreator

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtil
import com.jetbrains.edu.learning.EduUtils

class HintTemplateProvider : DefaultLiveTemplatesProvider {
  override fun getDefaultLiveTemplateFiles() = arrayOf("liveTemplates/hint.xml")

  override fun getHiddenLiveTemplateFiles() = emptyArray<String>()
}

class HintTemplateContentType : TemplateContextType("EDU_TASK_DESCRIPTION_HINT", "&Task description file") {
  override fun isInContext(file: PsiFile, offset: Int): Boolean {
    val project = PsiUtil.getProjectInReadAction(file)
    return CCUtils.isCourseCreator(project) && EduUtils.isTaskDescriptionFile(file.name)
  }
}