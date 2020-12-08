package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.util.ProcessingContext
import com.jetbrains.edu.codeInsight.EduPsiReferenceProvider
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
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

    override fun createFileReference(range: TextRange?, index: Int, text: String?): FileReference? {
      return super.createFileReference(range, index, text)?.let(::InCourseLinkReference)
    }

    override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem> = Condition { item ->
      val project = item.project
      val file = item.virtualFile
      if (file.isSectionDirectory(project) || file.isLessonDirectory(project) || file.isTaskDirectory(project)) return@Condition true
      val task = file.getContainingTask(project) ?: return@Condition false
      val configurator = project.course?.configurator ?: return@Condition false
      val relativePath = file.pathRelativeToTask(project)
      if (configurator.isTestFile(task, relativePath)) return@Condition false
      if (!file.isDirectory) task.getTaskFile(relativePath) != null else true
    }

    override fun getDefaultContexts(): Collection<PsiFileSystemItem> {
      val project = element.project
      return listOfNotNull(PsiManager.getInstance(project).findDirectory(project.courseDir))
    }
  }
}

private class InCourseLinkReference(fileReference: FileReference) : FileReference(fileReference) {

  override fun createLookupItem(candidate: PsiElement): Any? {
    if (candidate !is PsiDirectory) return super.createLookupItem(candidate)
    val file = candidate.virtualFile
    val project = candidate.project
    // In-course link cannot refer to some directory inside task directory
      // so it always makes sense to add `/` while completion of such items
      return if (!file.isTaskDirectory(project) && file.getContainingTask(project) != null) {
        LookupElementBuilder.createWithSmartPointer("${candidate.name}/", candidate)
          .withIcon(candidate.getIcon(0))
      }
      else {
        super.createLookupItem(candidate)

    }
  }
}
