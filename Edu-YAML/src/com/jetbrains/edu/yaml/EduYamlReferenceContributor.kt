package com.jetbrains.edu.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.util.ProcessingContext
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

class EduYamlReferenceContributor : PsiReferenceContributor() {

  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(taskFilePathPattern, EduYamlTaskFilePathReferenceProvider())
  }
}


private class EduYamlTaskFilePathReferenceProvider : PsiReferenceProvider() {

  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<FileReference> {
    val scalar = element as? YAMLScalar ?: return emptyArray()
    val taskDir = scalar.containingFile.originalFile.virtualFile?.parent ?: return emptyArray()
    return TaskFileReferenceSet(taskDir, scalar).allReferences
  }
}

private class TaskFileReferenceSet(private val taskDir: VirtualFile, element: YAMLScalar) : FileReferenceSet(element) {
  override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem> {
    return Condition { item ->
      if (item.isDirectory) return@Condition true
      val virtualFile = item.virtualFile
      // Do not suggest files that cannot be in course
      if (element.project.excludeFromArchive(virtualFile)) return@Condition false
      val filePaths = (element as YAMLScalar).collectFilePaths()
      val itemPath = VfsUtil.getRelativePath(virtualFile, taskDir) ?: return@Condition false
      itemPath !in filePaths
    }
  }

  private fun YAMLScalar.collectFilePaths(): List<String> {
    val sequence = parentOfType<YAMLSequence>() ?: return emptyList()
    return sequence.items.mapNotNull { item -> item.keysValues.find { it.keyText == "name" }?.valueText }
  }
}

private val taskFilePathPattern: PsiElementPattern.Capture<YAMLScalar> get() = psiElement<YAMLScalar>()
  .inFileWithName(YamlFormatSettings.TASK_CONFIG)
  .withParent(
    keyValueWithName("name").inside(keyValueWithName("files"))
  )

private fun Project.excludeFromArchive(virtualFile: VirtualFile): Boolean {
  val course = StudyTaskManager.getInstance(this).course ?: return true
  val configurator = course.configurator ?: return true
  return configurator.excludeFromArchive(this, virtualFile)
}
