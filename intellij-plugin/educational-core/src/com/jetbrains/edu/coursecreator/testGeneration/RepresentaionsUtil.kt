package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.openapi.project.Project
import org.jetbrains.research.testspark.core.generation.llm.prompt.configuration.ClassRepresentation
import org.jetbrains.research.testspark.core.generation.llm.prompt.configuration.MethodRepresentation

fun PsiMethodWrapper.toMethodRepresentation(): MethodRepresentation? {
  text ?: return null
  return MethodRepresentation(
    signature = signature,
    name = name,
    text = text!!,
    containingClassQualifiedName = containingClass!!.qualifiedName,
  )
}

fun PsiClassWrapper.toClassRepresentation(): ClassRepresentation {
  return ClassRepresentation(
    qualifiedName,
    fullText,
    allMethods.map { it.toMethodRepresentation() }.toList().filterNotNull(),
  )
}

fun getPolymorphismRelationsWithQualifiedNames(
  project: Project,
  interestingPsiClasses: MutableSet<PsiClassWrapper>,
  cutPsiClass: PsiClassWrapper,
): MutableMap<PsiClassWrapper, MutableList<PsiClassWrapper>> {
  val polymorphismRelations: MutableMap<PsiClassWrapper, MutableList<PsiClassWrapper>> = mutableMapOf()

  interestingPsiClasses.add(cutPsiClass)

  interestingPsiClasses.forEach { currentInterestingClass ->
    val detectedSubClasses = currentInterestingClass.searchSubclasses(project)

    detectedSubClasses.forEach { detectedSubClass ->
      if (!polymorphismRelations.contains(currentInterestingClass)) {
        polymorphismRelations[currentInterestingClass] = ArrayList()
      }
      polymorphismRelations[currentInterestingClass]?.add(detectedSubClass)
    }
  }

  interestingPsiClasses.remove(cutPsiClass)

  return polymorphismRelations.toMutableMap()
}
