package com.josephggd.importindex

import com.intellij.ide.plugins.PluginManager
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.*
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.event.ListSelectionEvent


internal class FileToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = FileToolWindowContent(project)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
    private class FileToolWindowContent(project:Project) : FileLogic(project) {
        val mainPanel = JPanel()
        var psiFiles = emptyList<PsiJavaFile>()
        var imports = emptyList<String>()
        var selectedImport=""
        var importingFiles = emptyList<String>()
        var selectedFileName:String?="no file selected"
        var logger = com.intellij.openapi.diagnostic.Logger.getFactory().getLoggerInstance("LOG")
        init {
            refreshImportStatements()
            mainPanel.layout = BorderLayout(0,0)
            mainPanel.border = BorderFactory.createEmptyBorder(20,20,20,20)
            mainPanel.add(createControlPanel(), BorderLayout.NORTH)

            val importSearch = createImportSearch(
                "loading"
            )
            mainPanel.add(importSearch, BorderLayout.WEST)

            val fileSearch = createFileSearch(
                "select an import"
            )
            mainPanel.add(fileSearch, BorderLayout.CENTER)

            val fileView = JLabel(selectedFileName)
            mainPanel.add(fileView, BorderLayout.EAST)
        }
        fun gatherUniqueImports():List<String>{
            val set = mutableSetOf<String>()
            for (psiFile in psiFiles) {
                val list:Collection<String> = psiFile
                    .importList
                    ?.importStatements
                    ?.map { it.qualifiedName?:"" }
                    ?: emptyList()
                set.addAll(list)
            }
            return set.toList().sorted()
        }
        fun gatherFilesWithImport(query:String):List<String>{
            val filesThatImportThisDep = psiFiles
                .filter { psiFile -> (psiFile.importList?.importStatements?.map { statement->statement.qualifiedName }?: emptyList()).contains(query) }
            return filesThatImportThisDep.map { it.name }.toList().sorted()
        }
        fun refreshImportStatements(){
            psiFiles = getProjectLevelImports()
            imports = gatherUniqueImports()
        }
        fun createImportSearch(loading:String) : JBScrollPane {
            val jbl = JBList(imports)
            jbl.setEmptyText(loading)
            jbl.selectionMode=ListSelectionModel.SINGLE_SELECTION
            jbl.addListSelectionListener {
                ApplicationManager.getApplication().invokeLater {
                    selectedImport = imports[it.lastIndex]
                    mainPanel.remove(2)

                    importingFiles = gatherFilesWithImport(selectedImport)

                    val fileSearch = createFileSearch(
                        "select an import",
                    )
                    mainPanel.add(fileSearch, BorderLayout.CENTER)
                    mainPanel.repaint()
                }
            }
            val lss = ListSpeedSearch(jbl)
            val jbsp = JBScrollPane(lss.component)
            return jbsp
        }
        fun createFileSearch(loading:String) : JBScrollPane {
            val jbl = JBList(importingFiles)
            jbl.setEmptyText(loading)
            jbl.selectionMode=ListSelectionModel.SINGLE_SELECTION
            jbl.addListSelectionListener {
                ApplicationManager.getApplication().invokeLater {
                    val newF = jbl.selectedValue
                    psiFiles.find { it.name==newF }?.navigate(true)
                }
            }
            val lss = ListSpeedSearch(jbl)
            val jbsp = JBScrollPane(lss.component)
            return jbsp
        }
        fun createControlPanel():JPanel {
            val jp = JPanel()

            val refreshFilesButton = JButton("Refresh Imports")
            refreshFilesButton.addActionListener {
                refreshImportStatements()
            }
            jp.add(refreshFilesButton, BorderLayout.PAGE_END)

            return jp
        }
    }
}
