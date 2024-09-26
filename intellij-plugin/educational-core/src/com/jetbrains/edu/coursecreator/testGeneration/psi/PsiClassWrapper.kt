package com.jetbrains.edu.coursecreator.testGeneration.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

/**
 * Interface representing a wrapper for PSI classes,
 * providing common API to handle class-related data for different languages.
 * @property name The name of a class
 * @property qualifiedName The qualified name of the class.
 * @property text The text of the class.
 * @property fullText The source code of the class (with package and imports).
 * @property virtualFile
 * @property containingFile File where the method is located
 * @property superClass The super class of the class
 * @property methods All methods in the class
 * @property allMethods All methods in the class and all its superclasses
 * */
interface PsiClassWrapper {
  val name: String
  val qualifiedName: String
  val text: String?
  val methods: List<PsiMethodWrapper>
  val allMethods: List<PsiMethodWrapper>
  val superClass: PsiClassWrapper?
  val virtualFile: VirtualFile
  val containingFile: PsiFile
  val fullText: String

  /**
   * Searches for subclasses of the current class within the given project.
   *
   * @param project The project within which to search for subclasses.
   * @return A collection of found subclasses.
   */
  fun searchSubclasses(project: Project): Collection<PsiClassWrapper>

  /**
   * Retrieves a set of interesting PSI classes based on a given method.
   *
   * @param psiMethod The method to use for finding interesting PSI classes.
   * @return A mutable set of interesting PSI classes.
   */
  fun getInterestingPsiClassesWithQualifiedNames(psiMethod: PsiMethodWrapper): MutableSet<PsiClassWrapper>
}
