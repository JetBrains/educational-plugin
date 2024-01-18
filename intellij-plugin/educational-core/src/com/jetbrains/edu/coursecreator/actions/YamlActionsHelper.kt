package com.jetbrains.edu.coursecreator.actions

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

interface YamlActionsHelper {

  /**
   * Finds the PSI element corresponding to the given answer placeholder in the task-info.yaml file.
   * Normally that would be a PSI element that represents the `placeholder_text` value, as in the example:
   * (The element is surrounded with the `<psi>` tag)
   *
   * ```yaml
   * placeholders:
   *       - offset: 32
   *         length: 6
   *         placeholder_text: <psi>TODO()</psi>
   *         is_visible: false
   * ```
   *
   * But if it is not found for some reason, the overall placeholder element is returned:
   *
   * ```yaml
   * placeholders:
   *       - <psi>offset: 32
   *         length: 6
   *         is_visible: false</psi>
   *       - offset: 48
   *         length: 6
   *         is_visible: false
   * ```
   *
   * @param taskInfoYamlPsiFile The PSI file of the task info YAML.
   * @param placeholder The answer placeholder for which to find the corresponding PSI element.
   * @return The PSI element corresponding to the placeholder, or null if not found.
   */
  fun findPsiElementCorrespondingToPlaceholder(taskInfoYamlPsiFile: PsiFile, placeholder: AnswerPlaceholder): NavigatablePsiElement?
}