package com.jetbrains.edu.java

import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.execution.JavaExecutionUtil
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiMethodUtil
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.checker.gradle.GradleTaskCheckerProvider

class JTaskCheckerProvider : GradleTaskCheckerProvider() {

    override fun mainClassForFile(project: Project, file: VirtualFile): String? {
        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (psiFile == null) {
            println("PsiFile ${file.name} not found")
            return null
        }

        val classes = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
        for (clazz in classes) {
            println("Class found: ${clazz.text}")
        }
        val mainClass = classes.find { psiClass ->
            isMainClass(psiClass) && hasMainMethod(psiClass)
        }
        if (mainClass == null) {
            println("Main class in ${psiFile.name} not found")
            return null
        }

        val runtimeQualifiedName = JavaExecutionUtil.getRuntimeQualifiedName(mainClass)
        if (runtimeQualifiedName == null) {
            println("Runtime qualified name not found")
        }
        return runtimeQualifiedName
    }

    private fun isMainClass(psiClass: PsiClass?): Boolean {
        val isMainClass = PsiMethodUtil.MAIN_CLASS.value(psiClass)
        println("isMainClass=$isMainClass")
        return isMainClass
    }

    fun hasMainMethod(psiClass: PsiClass): Boolean {
        val providers = Extensions.getExtensions(JavaMainMethodProvider.EP_NAME)
        if (providers.isEmpty()) {
            println("No providers")
        }
        for (provider in providers) {
            if (provider.isApplicable(psiClass)) {
                return provider.hasMainMethod(psiClass)
            }
        }
        return findMainMethod(psiClass.findMethodsByName("main", true)) != null
    }

    private fun findMainMethod(mainMethods: Array<PsiMethod>): PsiMethod? {
        for (mainMethod in mainMethods) {
            println("Main method: ${mainMethod.text}")
            val isMainMethod = isMainMethod(mainMethod)
            println("isMainMethod: $isMainMethod")
            if (isMainMethod) return mainMethod
        }
        return null
    }

    fun isMainMethod(method: PsiMethod?): Boolean {
        if (method == null || method.containingClass == null) {
            if (method != null) {
                println("Method: ${method.text}")
                println("Method has no containing class")
            } else {
                println("Method is null")
            }
            return false
        }
        if (PsiType.VOID != method.returnType) {
            println("Method return type: ${method.returnType}")
            return false
        }
        if (!method.hasModifierProperty(PsiModifier.STATIC)) {
            println("No static modifier. Modifiers: ${method.modifiers}")
            return false
        }
        if (!method.hasModifierProperty(PsiModifier.PUBLIC)) {
            println("No public modifier. Modifiers: ${method.modifiers}")
            return false
        }
        val parameters = method.parameterList.parameters
        if (parameters.size != 1) {
            println("Parameter size is ${parameters.size}")
            return false
        }
        val type = parameters[0].type as? PsiArrayType ?: return false
        println("Parameters size is OK")
        val componentType = type.componentType

        val equalsToText = componentType.equalsToText(CommonClassNames.JAVA_LANG_STRING)
        if (!equalsToText) {
            println("Component type: ${componentType.canonicalText}")
        }
        return equalsToText
    }
}
