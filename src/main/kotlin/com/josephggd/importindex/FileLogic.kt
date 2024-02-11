package com.josephggd.importindex

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import javax.swing.tree.DefaultMutableTreeNode


internal open class FileLogic(val project: Project) {
    private val JAVA = "java"

    fun getFileLevelImports(virtualFile:VirtualFile?):List<PsiJavaFile>{
        val fileLevelImports = mutableListOf<PsiJavaFile>()
        if (virtualFile!=null) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            if (psiFile is PsiJavaFile) {
                fileLevelImports.add(psiFile)
            }
        }
        return fileLevelImports
    }
    fun getProjectLevelImports():List<PsiJavaFile> {
        val allImports = mutableListOf<PsiJavaFile>()
        val virtualFiles =
            FilenameIndex.getAllFilesByExt(project, JAVA, GlobalSearchScope.projectScope(project))
        for (vf in virtualFiles) {
            val projectLevelImports = getFileLevelImports(vf)
            allImports.addAll(projectLevelImports)
        }
        return allImports
    }
}
