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
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
        val mainClass = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java).find { psiClass ->
            PsiMethodUtil.MAIN_CLASS.value(psiClass) && PsiMethodUtil.hasMainMethod(psiClass)
        } ?: return null

        return JavaExecutionUtil.getRuntimeQualifiedName(mainClass)
    }
}
