package com.jetbrains.edu.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.util.ProcessingContext
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.TASK_CONFIG
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

class EduYamlReferenceContributor : PsiReferenceContributor() {

  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerEduReferenceProvider(EduYamlTaskFilePathReferenceProvider())
    registrar.registerEduReferenceProvider(ItemContainerContentReferenceProvider())
  }

  private fun PsiReferenceRegistrar.registerEduReferenceProvider(provider: EduPsiReferenceProvider) {
    registerReferenceProvider(provider.pattern, provider)
  }
}

private abstract class EduPsiReferenceProvider : PsiReferenceProvider() {
  abstract val pattern: ElementPattern<out PsiElement>
}

private class EduYamlTaskFilePathReferenceProvider : EduPsiReferenceProvider() {

  override val pattern: PsiElementPattern.Capture<YAMLScalar> = psiElement<YAMLScalar>()
    .inFileWithName(TASK_CONFIG)
    .withParent(
      keyValueWithName("name").inside(keyValueWithName("files"))
    )

  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<FileReference> {
    val scalar = element as? YAMLScalar ?: return emptyArray()
    val taskDir = scalar.containingFile.originalFile.virtualFile?.parent ?: return emptyArray()
    return TaskFileReferenceSet(taskDir, scalar).allReferences
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
}

private class ItemContainerContentReferenceProvider : EduPsiReferenceProvider() {

  override val pattern: PsiElementPattern.Capture<YAMLScalar> = psiElement<YAMLScalar>()
    .inFileWithName(COURSE_CONFIG, SECTION_CONFIG, LESSON_CONFIG)
    .withParent(
      psiElement<YAMLSequenceItem>().inside(keyValueWithName("content"))
    )

  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<FileReference> {
    val scalar = element as? YAMLScalar ?: return emptyArray()
    val itemContainerDir = scalar.containingFile.originalFile.virtualFile?.parent ?: return emptyArray()
    return ContentReferenceSet(itemContainerDir, scalar).allReferences
  }

  private class ContentReferenceSet(private val itemContainerDir: VirtualFile, element: YAMLScalar) : FileReferenceSet(element) {
    override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem> {
      return Condition { item ->
        if (!item.isDirectory) return@Condition false
        val containingFile = containingFile ?: return@Condition false
        // Suggest only direct children
        if (containingFile.parent?.findSubdirectory(item.name) != item) return@Condition false

        val virtualFile = item.virtualFile
        // Do not suggest files that cannot be in course
        if (item.project.excludeFromArchive(virtualFile)) return@Condition false

        val contentPaths = (element as YAMLScalar).collectContentPaths()
        val itemPath = VfsUtil.getRelativePath(virtualFile, itemContainerDir) ?: return@Condition false
        itemPath !in contentPaths
      }
    }

    private fun YAMLScalar.collectContentPaths(): List<String> {
      val sequence = parentOfType<YAMLSequence>() ?: return emptyList()
      return sequence.items.mapNotNull { (it.value as? YAMLScalar)?.textValue }
    }
  }
}

private fun Project.excludeFromArchive(virtualFile: VirtualFile): Boolean {
  val course = StudyTaskManager.getInstance(this).course ?: return true
  val configurator = course.configurator ?: return true
  return configurator.excludeFromArchive(this, virtualFile)
}
