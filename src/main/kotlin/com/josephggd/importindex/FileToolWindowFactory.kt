package com.josephggd.importindex

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiJavaFile
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
        val importJL = JBList(emptyList<String>())
        val fileJL = JBList(emptyList<PsiFileExtender>())
        var importToFileMap = emptyMap<String,List<PsiJavaFile>>()
        var importingFiles = emptyList<PsiJavaFile>()
        var selectedFileName:String?="no file selected"
        var logger = com.intellij.openapi.diagnostic.Logger.getFactory().getLoggerInstance("Import Index")
        init {
            refreshImportStatements()
            mainPanel.layout = BorderLayout(0,0)
            mainPanel.border = BorderFactory.createEmptyBorder(20,20,20,20)
            mainPanel.add(createControlPanel(), BorderLayout.NORTH)
            mainPanel.add(createImportSearch(), BorderLayout.WEST)
            mainPanel.add(createFileSearch(), BorderLayout.CENTER)
            val fileView = JLabel(selectedFileName)
            mainPanel.add(fileView, BorderLayout.EAST)
        }
        fun getSelectedIndex(lse:ListSelectionEvent):Int{
            val list = lse.getSource() as JList<*>
            val selected = list.selectedIndex
            return selected
        }

        fun importCallback(lse:ListSelectionEvent) {
            ApplicationManager.getApplication().invokeAndWait {
                val index = getSelectedIndex(lse)
                if (!lse.valueIsAdjusting) {
                    val impName = importJL.model.getElementAt(index)
                    fileJL.selectedIndex=-1
                    fileJL.setListData(
                        importToFileMap
                            .getOrDefault(impName, emptyList<PsiJavaFile>())
                            .map { PsiFileExtender(it) }
                            .toTypedArray()
                    )
                }
            }
        }
        fun fileCallback(lse:ListSelectionEvent){
            ApplicationManager.getApplication().invokeLater {
                val index = getSelectedIndex(lse)
                if (!lse.valueIsAdjusting) {
                    val newF:PsiFileExtender = fileJL.model.getElementAt(index)
                    newF.navigate()
                }
            }
        }
        fun refreshImportStatements(){
            ApplicationManager.getApplication().invokeAndWait {
                importToFileMap = getImportsAsMap()
                importJL.selectedIndex=-1
                importingFiles= emptyList()
                selectedFileName=""
            }
        }
        fun createImportSearch() : JBScrollPane {
            importJL.setEmptyText("loading")
            importJL.setListData(importToFileMap.keys.sorted().toTypedArray())
            importJL.selectionMode=ListSelectionModel.SINGLE_SELECTION
            importJL.addListSelectionListener {
                importCallback(it)
            }
            val lss = ListSpeedSearch(importJL)
            val jbsp = JBScrollPane(lss.component)
            return jbsp
        }
        fun createFileSearch() : JBScrollPane {
            val fl = importingFiles.map { PsiFileExtender(it) }.toTypedArray()
            fileJL.setEmptyText("select an import")
            fileJL.setListData(fl)
            fileJL.selectionMode=ListSelectionModel.SINGLE_SELECTION
            fileJL.addListSelectionListener {
                fileCallback(it)
            }
            val lss = ListSpeedSearch(fileJL)
            val jbsp = JBScrollPane(lss.component)
            return jbsp
        }
        fun createControlPanel():JPanel {
            val jp = JPanel()
            val refreshFilesButton = JButton("Refresh Imports")
            refreshFilesButton.isEnabled=false
            refreshFilesButton.addActionListener {
                refreshImportStatements()
                refreshFilesButton.isEnabled=false
            }
            jp.add(refreshFilesButton, BorderLayout.PAGE_END)
            return jp
        }
    }
}
