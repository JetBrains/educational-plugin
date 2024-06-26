package com.jetbrains.edu.java.testGeneration

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.util.containers.stream
import com.jetbrains.edu.coursecreator.testGeneration.PsiClassWrapper
import com.jetbrains.edu.coursecreator.testGeneration.PsiMethodWrapper
import java.util.stream.Collectors

class JavaPsiMethodWrapper(private val psiMethod: PsiMethod) : PsiMethodWrapper {
    override val name: String get() = psiMethod.name

    override val text: String? = psiMethod.text

    override val containingClass: PsiClassWrapper? = psiMethod.containingClass?.let { JavaPsiClassWrapper(it) }

    override val containingFile: PsiFile = psiMethod.containingFile

    override val methodDescriptor: String
        get() {
            val parameterTypes =
                psiMethod.getSignature(PsiSubstitutor.EMPTY)
                    .parameterTypes
                    .stream()
                    .map { i -> generateFieldType(i) }
                    .collect(Collectors.joining())

            val returnType = generateReturnDescriptor(psiMethod)

            return "${psiMethod.name}($parameterTypes)$returnType"
        }

    override val signature: String
        get() {
            val bodyStart = psiMethod.body?.startOffsetInParent ?: psiMethod.textLength
            return psiMethod.text.substring(0, bodyStart).replace('\n', ' ').trim()
        }

    val parameterList = psiMethod.parameterList

    val isConstructor: Boolean = psiMethod.isConstructor

    val isMethodDefault: Boolean
        get() {
            if (psiMethod.body == null) return false
            return psiMethod.containingClass?.isInterface ?: return false
        }

    val isDefaultConstructor: Boolean get() = psiMethod.isConstructor && (psiMethod.body?.isEmpty ?: false)

    override fun containsLine(lineNumber: Int): Boolean {
        val psiFile = psiMethod.containingFile ?: return false
        val document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile) ?: return false
        val textRange = psiMethod.textRange
        val startLine = document.getLineNumber(textRange.startOffset) + 1
        val endLine = document.getLineNumber(textRange.endOffset) + 1
        return lineNumber in startLine..endLine
    }

    /**
     * Generates the return descriptor for a method.
     *
     * @param psiMethod the method
     * @return the return descriptor
     */
    private fun generateReturnDescriptor(psiMethod: PsiMethod): String {
        if (psiMethod.returnType == null || psiMethod.returnType!!.canonicalText == "void") {
            // void method
            return "V"
        }

        return generateFieldType(psiMethod.returnType!!)
    }

    /**
     * Generates the field descriptor for a type.
     *
     * @param psiType the type to generate the descriptor for
     * @return the field descriptor
     */
    private fun generateFieldType(psiType: PsiType): String {
        // arrays (ArrayType)
        if (psiType.arrayDimensions > 0) {
            val arrayType = generateFieldType(psiType.deepComponentType)
            return "[".repeat(psiType.arrayDimensions) + arrayType
        }

        //  objects (ObjectType)
        if (psiType is PsiClassType) {
            val classType = psiType.resolve()
            if (classType != null) {
                val className = classType.qualifiedName?.replace('.', '/')

                // no need to handle generics: they are not part of method descriptors

                return "L$className;"
            }
        }

        // primitives (BaseType)
        psiType.canonicalText.let {
            return when (it) {
                "int" -> "I"
                "long" -> "J"
                "float" -> "F"
                "double" -> "D"
                "boolean" -> "Z"
                "byte" -> "B"
                "char" -> "C"
                "short" -> "S"
                else -> throw IllegalArgumentException("Unknown type: $it")
            }
        }
    }
}
