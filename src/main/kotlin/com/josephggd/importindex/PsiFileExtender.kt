package com.josephggd.importindex

import com.intellij.psi.PsiJavaFile
data class PsiFileExtender(val psiFile: PsiJavaFile) {
    override fun toString(): String {
        return psiFile.name
    }
    fun navigate(){
        psiFile.navigate(true)
    }
}
