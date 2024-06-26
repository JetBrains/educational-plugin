package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.lang.Language
import com.intellij.psi.PsiFile

/**
 * Interface that declares all the methods needed for parsing and
 * handling the PSI (Program Structure Interface) for different languages.
 */
interface PsiHelper {
  var psiFile: PsiFile?
  val language: TestLanguage

  /**
   * Returns the surrounding PsiClass object based on the caret position within the specified PsiFile.
   * The surrounding class is determined by finding the PsiClass objects within the PsiFile and checking
   * if the caret is within any of them.
   *
   * @param caretOffset The offset of the caret position within the PsiFile.
   * @return The surrounding PsiClass object if found, null otherwise.
   */
  fun getSurroundingClass(caretOffset: Int): PsiClassWrapper?

  /**
   * Returns the surrounding method of the given PSI file based on the caret offset.
   *
   * @param caretOffset The caret offset within the PSI file.
   * @return The surrounding method if found, otherwise null.
   */
  fun getSurroundingMethod(caretOffset: Int): PsiMethodWrapper?

  /**
   * Returns the line number of the selected line where the caret is positioned.
   *
   * @param caretOffset The caret offset within the PSI file.
   * @return The line number of the selected line, otherwise null.
   */
  fun getSurroundingLine(caretOffset: Int): Int?

  /**
   * Retrieves a set of interesting PsiClasses based on a given project,
   * a list of classes to test, and a depth reducing factor.
   *
   * @param project The project in which to search for interesting classes.
   * @param classesToTest The list of classes to test for interesting PsiClasses.
   * @param polyDepthReducing The factor to reduce the polymorphism depth.
   * @return The set of interesting PsiClasses found during the search.
   */
  fun getInterestingPsiClassesWithQualifiedNames(
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
  fun getInterestingPsiClassesWithQualifiedNames(
    cut: PsiClassWrapper,
    psiMethod: PsiMethodWrapper,
  ): MutableSet<PsiClassWrapper>

  /**
   * Gets the current list of code types based on the given AnActionEvent.
   *
   * @param e The AnActionEvent representing the current action event.
   * @return An array containing the current code types. If no caret or PSI file is found, an empty array is returned.
   *         The array contains the class display name, method display name (if present), and the line number (if present).
   *         The line number is prefixed with "Line".
   */
  fun getCurrentListOfCodeTypes(e: AnActionEvent): Array<*>?

  /**
   * Helper for generating method descriptors for methods.
   *
   * @param psiMethod The method to extract the descriptor from.
   * @return The method descriptor.
   */
  fun generateMethodDescriptor(psiMethod: PsiMethodWrapper): String

  /**
   * Fills the classesToTest variable with the data about the classes to test.
   *
   * @param project The project in which to collect classes to test.
   * @param classesToTest The list of classes to test.
   * @param psiHelper The PSI helper instance to use for collecting classes.
   * @param caretOffset The caret offset in the file.
   */
  fun collectClassesToTest(
    project: Project,
    classesToTest: MutableList<PsiClassWrapper>,
    caretOffset: Int,
  )

  /**
   * Gets the display line number.
   * This is used when displaying the name of a method in the GenerateTestsActionMethod menu entry.
   *
   * @param line The line number.
   * @return The display name of the line.
   */
  fun getLineDisplayName(line: Int): String

  /**
   * Gets the display name of a class.
   * This is used when displaying the name of a class in the GenerateTestsActionClass menu entry.
   *
   * @param psiClass The PSI class of interest.
   * @return The display name of the PSI class.
   */
  fun getClassDisplayName(psiClass: PsiClassWrapper): String

  /**
   * Gets the display name of a method, depending on if it is a (default) constructor or a normal method.
   * This is used when displaying the name of a method in the GenerateTestsActionMethod menu entry.
   *
   * @param psiMethod The PSI method of interest.
   * @return The display name of the PSI method.
   */
  fun getMethodDisplayName(psiMethod: PsiMethodWrapper): String


  fun getPackageName(): String

  companion object{
    private val EP_NAME = LanguageExtension<PsiHelper>("Educational.psiHelper")

    fun getInstance(language: Language): PsiHelper = EP_NAME.forLanguage(language)
  }

}
