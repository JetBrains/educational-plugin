package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.util.ProcessingContext
import com.jetbrains.edu.codeInsight.EduPsiReferenceProvider
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.taskDescription.ui.ToolWindowLinkHandler

abstract class InCourseLinkReferenceProviderBase : EduPsiReferenceProvider() {

  protected abstract val PsiElement.value: String?

  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<FileReference> {
    val value = element.value ?: return emptyArray()
    return if (value.startsWith(ToolWindowLinkHandler.IN_COURSE_PROTOCOL)) {
      val path = value.substringAfter(ToolWindowLinkHandler.IN_COURSE_PROTOCOL)
      CourseFileReferenceSet(path, element).allReferences
    }
    else {
      emptyArray()
    }
  }

  private inner class CourseFileReferenceSet(str: String, element: PsiElement) :
    FileReferenceSet(str, element, ToolWindowLinkHandler.IN_COURSE_PROTOCOL.length, this, true) {

    override fun getDefaultContexts(): Collection<PsiFileSystemItem> {
      val project = element.project
      return listOfNotNull(PsiManager.getInstance(project).findDirectory(project.courseDir))
    }
  }
}
