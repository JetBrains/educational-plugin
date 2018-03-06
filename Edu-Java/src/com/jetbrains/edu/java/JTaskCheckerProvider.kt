package com.jetbrains.edu.java

import com.intellij.execution.JavaExecutionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
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
            PsiMethodUtil.MAIN_CLASS.value(psiClass) && PsiMethodUtil.hasMainMethod(psiClass)
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
}
