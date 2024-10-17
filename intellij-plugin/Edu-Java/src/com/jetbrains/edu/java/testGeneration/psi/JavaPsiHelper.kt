package com.jetbrains.edu.java.testGeneration.psi

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiClassWrapper
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiHelper
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiMethodWrapper
import com.jetbrains.edu.coursecreator.testGeneration.psi.TestLanguage
import com.jetbrains.edu.coursecreator.testGeneration.util.SettingsArguments

class JavaPsiHelper(private val psiFile: PsiFile) : PsiHelper() {
  override val language: TestLanguage get() = TestLanguage.Java

  private val log = Logger.getInstance(this::class.java)

  override fun generateMethodDescriptor(
    psiMethod: PsiMethodWrapper,
  ): String {
    val methodDescriptor = psiMethod.methodDescriptor
    log.info("Method description: $methodDescriptor")
    return methodDescriptor
  }

  override fun getSurroundingClass(
    caretOffset: Int,
  ): PsiClassWrapper? =
    PsiTreeUtil.findChildrenOfAnyType(psiFile, PsiClass::class.java)
      .firstOrNull { cls -> cls.containsOffset(caretOffset) && JavaPsiClassWrapper(cls).isTestableClass() }
      ?.let { cls ->
        log.info("Surrounding class for caret in $caretOffset is ${cls.qualifiedName}")
        JavaPsiClassWrapper(cls)
      } ?: run {
      log.info("No surrounding class for caret in $caretOffset")
      null
    }


  override fun getSurroundingMethod(
    caretOffset: Int,
  ): PsiMethodWrapper? =
    PsiTreeUtil.findChildrenOfAnyType(psiFile, PsiMethod::class.java)
      .filter { method -> method.body != null && method.containsOffset(caretOffset) }.firstNotNullOfOrNull { method ->
        val surroundingClass = PsiTreeUtil.getParentOfType(method, PsiClass::class.java)
        val surroundingClassWrapper = surroundingClass?.let { JavaPsiClassWrapper(it) }
        if (surroundingClassWrapper?.isTestableClass() == true) {
          JavaPsiMethodWrapper(method).also {
            log.info("Surrounding method for caret in $caretOffset is ${it.methodDescriptor}")
          }
        }
        else {
          null
        }
      }
    ?: run {
      log.info("No surrounding method for caret in $caretOffset")
      null
    }

  override fun getSurroundingLine(
    caretOffset: Int,
  ): Int? {
    val doc = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile) ?: return null

    val selectedLine = doc.getLineNumber(caretOffset)
    val selectedLineText =
      doc.getText(TextRange(doc.getLineStartOffset(selectedLine), doc.getLineEndOffset(selectedLine)))

    return if (selectedLineText.isBlank()) {
      null.also { log.info("Line $selectedLine at caret $caretOffset is blank") }
    }
    else {
      // increase by one is necessary due to different start of numbering
      (selectedLine + 1).also { log.info("Surrounding line at caret $caretOffset is $selectedLine") }
    }
  }

  override fun collectClassesToTest(
    project: Project,
    classesToTest: MutableSet<PsiClassWrapper>,
    caretOffset: Int,
  ) {
    // check if cut has any none java super class
    val maxPolymorphismDepth = 5 //TODO

    val cutPsiClass = getSurroundingClass(caretOffset) ?: return
    var currentPsiClass = cutPsiClass
    repeat(maxPolymorphismDepth) {
      classesToTest.add(currentPsiClass)

      if (currentPsiClass.superClass?.qualifiedName?.startsWith("java.") != false) {
        return@repeat
      }
      currentPsiClass = currentPsiClass.superClass ?: return
    }
    log.info("There are ${classesToTest.size} classes to test")
  }

  override fun getInterestingPsiClassesWithQualifiedNames(
    project: Project,
    classesToTest: List<PsiClassWrapper>,
    polyDepthReducing: Int,
  ): MutableSet<PsiClassWrapper> {
    val interestingPsiClasses: MutableSet<JavaPsiClassWrapper> = mutableSetOf()

    var currentLevelClasses = classesToTest.toMutableList()

    repeat(SettingsArguments(project).maxInputParamsDepth(polyDepthReducing)) {
      val tempListOfClasses = mutableSetOf<JavaPsiClassWrapper>()

      currentLevelClasses.forEach { classIt ->
        classIt.methods.forEach { methodIt ->
          (methodIt as JavaPsiMethodWrapper).parameterList.parameters.forEach { paramIt ->
            PsiTypesUtil.getPsiClass(paramIt.type)?.let { typeIt ->
              JavaPsiClassWrapper(typeIt).let {
                if (!it.qualifiedName.startsWith("java.")) {
                  interestingPsiClasses.add(it)
                }
              }
            }
          }
        }
      }
      currentLevelClasses = tempListOfClasses.toMutableList()
      interestingPsiClasses.addAll(tempListOfClasses)
    }
    log.info("There are ${interestingPsiClasses.size} interesting psi classes")
    return interestingPsiClasses.toMutableSet()
  }

  override fun getInterestingPsiClassesWithQualifiedNames(
    cut: PsiClassWrapper,
    psiMethod: PsiMethodWrapper,
  ): MutableSet<PsiClassWrapper> =
    cut.getInterestingPsiClassesWithQualifiedNames(psiMethod).also {
      log.info("There are ${it.size} interesting psi classes from method ${psiMethod.methodDescriptor}")
    }

  override fun getPackageName(): String {
    val psiPackage = JavaDirectoryService.getInstance().getPackage(psiFile.containingDirectory) // TODO
    return psiPackage?.qualifiedName ?: ""
  }

  private fun PsiElement.containsOffset(caretOffset: Int) = caretOffset in textRange

}
