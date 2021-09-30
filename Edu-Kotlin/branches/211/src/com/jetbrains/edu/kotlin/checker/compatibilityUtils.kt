package com.jetbrains.edu.kotlin.checker

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.run.KotlinJUnitRunConfigurationProducer

fun getTestClass(element: PsiElement): PsiClass? = KotlinJUnitRunConfigurationProducer.getTestClass(element)
