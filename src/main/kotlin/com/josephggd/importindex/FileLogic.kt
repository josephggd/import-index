package com.josephggd.importindex

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope


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

    fun getImportsAsMap():Map<String,MutableList<PsiJavaFile>>{
        val map = mutableMapOf<String,MutableList<PsiJavaFile>>()
        val virtualFiles =
            FilenameIndex.getAllFilesByExt(project, JAVA, GlobalSearchScope.projectScope(project))
        for (vf in virtualFiles) {
            val psiFile = PsiManager.getInstance(project).findFile(vf)
            if (psiFile is PsiJavaFile){
                for (stmt in (psiFile.importList?.importStatements?: emptyArray<PsiImportStatement>()).map { it?.qualifiedName?:"" }) {
                    val list:MutableList<PsiJavaFile> = map.getOrDefault(stmt, mutableListOf<PsiJavaFile>())
                    list.add(psiFile)
                    map[stmt] = list
                }
            }
        }
        return map
    }
}
