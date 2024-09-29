package com.jetbrains.edu.coursecreator.testGeneration.psi

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiFile

/**
 * Abstract class that declares all the methods needed for parsing and
 * handling the PSI (Program Structure Interface) for different languages.
 */
abstract class PsiHelper {
  abstract val language: TestLanguage

  /**
   * Returns the surrounding PsiClass object based on the caret position within the specified PsiFile.
   * The surrounding class is determined by finding the PsiClass objects within the PsiFile and checking
   * if the caret is within any of them.
   *
   * @param caretOffset The offset of the caret position within the PsiFile.
   * @return The surrounding PsiClass object if found, null otherwise.
   */
  abstract fun getSurroundingClass(caretOffset: Int): PsiClassWrapper?

  /**
   * Returns the surrounding method of the given PSI file based on the caret offset.
   *
   * @param caretOffset The caret offset within the PSI file.
   * @return The surrounding method if found, otherwise null.
   */
  abstract fun getSurroundingMethod(caretOffset: Int): PsiMethodWrapper?

  /**
   * Returns the line number of the selected line where the caret is positioned.
   *
   * @param caretOffset The caret offset within the PSI file.
   * @return The line number of the selected line, otherwise null.
   */
  abstract fun getSurroundingLine(caretOffset: Int): Int?

  /**
   * Retrieves a set of interesting PsiClasses based on a given project,
   * a list of classes to test, and a depth reducing factor.
   *
   * @param project The project in which to search for interesting classes.
   * @param classesToTest The list of classes to test for interesting PsiClasses.
   * @param polyDepthReducing The factor to reduce the polymorphism depth.
   * @return The set of interesting PsiClasses found during the search.
   */
  abstract fun getInterestingPsiClassesWithQualifiedNames(
    project: Project,
    classesToTest: List<PsiClassWrapper>,
    polyDepthReducing: Int,
  ): MutableSet<PsiClassWrapper>

  /**
   * Returns a set of interesting PsiClasses based on the given PsiMethod.
   *
   * @param cut The class under test.
   * @param psiMethod The PsiMethod for which to find interesting PsiClasses.
   * @return A mutable set of interesting PsiClasses.
   */
  abstract fun getInterestingPsiClassesWithQualifiedNames(
    cut: PsiClassWrapper,
    psiMethod: PsiMethodWrapper,
  ): MutableSet<PsiClassWrapper>

  /**
   * Helper for generating method descriptors for methods.
   *
   * @param psiMethod The method to extract the descriptor from.
   * @return The method descriptor.
   */
  abstract fun generateMethodDescriptor(psiMethod: PsiMethodWrapper): String

  /**
   * Fills the classesToTest variable with the data about the classes to test.
   *
   * @param project The project in which to collect classes to test.
   * @param classesToTest The list of classes to test.
   * @param caretOffset The caret offset in the file.
   */
  abstract fun collectClassesToTest(
    project: Project,
    classesToTest: MutableList<PsiClassWrapper>,
    caretOffset: Int,
  )

  abstract fun getPackageName(): String


  /**
   * Retrieves all classes to test based on the project and caret position.
   *
   * @param project The project in which to retrieve classes to test.
   * @param caret The caret position in the file.
   * @return The list of PsiClassWrapper objects representing the classes to test.
   */
  fun getAllClassesToTest(project: Project, caret: Int): List<PsiClassWrapper> {
    val classesToTest = mutableListOf<PsiClassWrapper>()
    ApplicationManager.getApplication().runReadAction(
      Computable {
        collectClassesToTest(project, classesToTest, caret)
      },
    )
    return classesToTest
  }

}
