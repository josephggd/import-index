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
    fun getPackages(): MutableSet<String> {
        val userDefinedPackages = mutableSetOf<String>()
        val virtualFiles =
            FilenameIndex.getAllFilesByExt(project, JAVA, GlobalSearchScope.projectScope(project))
        for (vf in virtualFiles) {
            val psiFile = PsiManager.getInstance(project).findFile(vf!!)
            if (psiFile is PsiJavaFile) {
                userDefinedPackages.add(psiFile.packageName)
            }
        }
        return userDefinedPackages
    }

    fun getFileLevelImports(virtualFile:VirtualFile?):List<Import>{
        val fileLevelImports = mutableListOf<Import>()
        if (virtualFile!=null) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            if (psiFile is PsiJavaFile) {
                val fileName = psiFile.name
                val pkgName = psiFile.packageName
                for (importStatement in psiFile.importList?.importStatements ?: emptyArray()) {
                    val importText = importStatement.text.split("import")[1]
                    val thisImport = Import(fileName, pkgName, importText)
                    fileLevelImports.add(thisImport)
                }
            }
        }
        return fileLevelImports
    }
    fun getProjectLevelImports():List<Import> {
        val allImports = mutableListOf<Import>()
        val virtualFiles =
            FilenameIndex.getAllFilesByExt(project, JAVA, GlobalSearchScope.projectScope(project))
        for (vf in virtualFiles) {
            val projectLevelImports = getFileLevelImports(vf)
            allImports.addAll(projectLevelImports)
        }
        return allImports
    }
    fun userDefinedPackage(psiFile:PsiFile):Boolean {
        if (psiFile is PsiJavaFile) {
            val packageName = psiFile.packageName
            val pkg = JavaPsiFacade.getInstance(project).findPackage(packageName)
            return pkg != null
        }
        return false
    }
    fun mapImportToTree(importName:String, imps:List<Import>, root:DefaultMutableTreeNode){
        for (import in imps.filter { it.imported == importName } ) {
            root.add(
                DefaultMutableTreeNode(import.importing)
            )
        }
    }
}
