package com.jetbrains.edu.java

import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.execution.JavaExecutionUtil
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
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
            val isMainMethod = PsiMethodUtil.isMainMethod(mainMethod)
            println("isMainMethod: $isMainMethod")
            if (isMainMethod) return mainMethod
        }
        return null
    }
}
