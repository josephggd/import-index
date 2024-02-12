package com.josephggd.importindex

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.components.*
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBImageIcon
import com.intellij.util.ui.UIUtil
import com.jetbrains.rd.swing.mouseClicked
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.Action.LARGE_ICON_KEY
import javax.swing.event.ListSelectionEvent


internal class FileToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = FileToolWindowContent(project)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
    private class FileToolWindowContent(project:Project) : FileLogic(project) {
        val mainPanel = JPanel()
        val stepOneCheckBox = JBCheckBox("Package only?")
        val stepOneSearchSelect = JBList(emptyList<String>())
        val stepTwoSearchSelect = JBList(emptyList<PsiFileExtender>())
        val stepTwoFileLink = JButton("", AllIcons.Ide.External_link_arrow)
        var importToFileMap = emptyMap<String,List<PsiJavaFile>>()
        var importingFiles = emptyList<PsiJavaFile>()
        var selectedFileName:String?="no file selected"
        var logger = Logger.getFactory().getLoggerInstance("Import Index")
        init {
            refreshImportStatements()
            mainPanel.layout = BorderLayout(20,20)
            mainPanel.border = BorderFactory.createEmptyBorder(20,20,20,20)
            project.messageBus.connect().subscribe<BulkFileListener>(
                VirtualFileManager.VFS_CHANGES,
                object : BulkFileListener {
                    override fun after(events: MutableList<out VFileEvent>) {
                        refreshImportStatements()
                    }
                })
            mainPanel.add(createImportSearch(), BorderLayout.WEST)
            mainPanel.add(createFileSearch(), BorderLayout.CENTER)
            val fileView = JLabel(selectedFileName)
            mainPanel.add(fileView, BorderLayout.EAST)
        }
        fun getSelectedIndex(lse:ListSelectionEvent):Int{
            val list = lse.source as JList<*>
            val selected = list.selectedIndex
            return selected
        }
        fun simplifyToPkg(str:String):String{
            val empty = mutableListOf<String>()
            val list = str.split(".")
            for (i in 0..1) {
                try {
                    empty.add(list[i])
                } catch (e:Exception) {
                    logger.warn("BAD PKG NAME")
                }
            }
            return empty.joinToString (".")
        }

        fun importCallback(lse:ListSelectionEvent) {
            ApplicationManager.getApplication().invokeAndWait {
                val index = getSelectedIndex(lse)
                if (!lse.valueIsAdjusting) {
                    stepTwoFileLink.isEnabled=false
                    val impName = stepOneSearchSelect.model.getElementAt(index)
                    if (!stepOneCheckBox.isSelected) {
                        stepTwoSearchSelect.setListData(
                            importToFileMap
                                .getOrDefault(impName, emptyList())
                                .map { PsiFileExtender(it) }
                                .toTypedArray()
                        )
                    } else {
                        val emptyList = mutableListOf<PsiFileExtender>()
                        for (eachImp in importToFileMap.keys) {
                            if (eachImp.contains(impName)) {
                                emptyList.addAll(
                                    importToFileMap
                                        .getOrDefault(eachImp, emptyList())
                                        .map { PsiFileExtender(it) })
                            }
                        }
                        stepTwoSearchSelect.setListData(emptyList.toSet().toTypedArray())
                    }
                }
            }
        }
        fun fileCallback(lse:ListSelectionEvent){
            ApplicationManager.getApplication().invokeLater {
                val index = getSelectedIndex(lse)
                if (!lse.valueIsAdjusting) {
                    val newF:PsiFileExtender = stepTwoSearchSelect.model.getElementAt(index)
                    stepTwoFileLink.isEnabled=true
                    stepTwoFileLink.addActionListener {
                        newF.navigate()
                    }
                }
            }
        }
        fun refreshImportStatements(){
            ApplicationManager.getApplication().invokeAndWait {
                importToFileMap = getImportsAsMap()
                stepOneSearchSelect.setListData(importToFileMap.keys.sorted().toTypedArray())
                importingFiles= emptyList()
                selectedFileName=""
            }
        }
        fun createImportSearch() : DialogPanel {
            val dp = DialogPanel("Project Imports:")
            stepOneSearchSelect.setEmptyText("Loading")
            stepOneSearchSelect.setListData(importToFileMap.keys.sorted().toTypedArray())
            stepOneSearchSelect.selectionMode=ListSelectionModel.SINGLE_SELECTION
            stepOneSearchSelect.addListSelectionListener {
                importCallback(it)
            }
            val lss = ListSpeedSearch(stepOneSearchSelect)
            val jbsp = JBScrollPane(lss.component)
            stepOneCheckBox.border=BorderFactory.createEmptyBorder(11,0,0,0)
            stepOneCheckBox.addChangeListener {
                if (!stepOneCheckBox.isSelected) {
                    stepOneSearchSelect.setListData(importToFileMap
                        .keys
                        .sorted()
                        .toTypedArray())
                } else {
                    stepOneSearchSelect.setListData(importToFileMap
                        .keys
                        .map { simplifyToPkg(it) }
                        .toSet()
                        .sorted()
                        .toTypedArray())
                }
            }
            dp.add(stepOneCheckBox, BorderLayout.NORTH)
            dp.add(jbsp)
            return dp
        }
        fun createFileSearch() : DialogPanel {
            val fl = importingFiles.map { PsiFileExtender(it) }.toTypedArray()
            stepTwoSearchSelect.setEmptyText("Select an Import")
            stepTwoSearchSelect.setListData(fl)
            stepTwoSearchSelect.selectionMode=ListSelectionModel.SINGLE_SELECTION
            stepTwoSearchSelect.addListSelectionListener {
                fileCallback(it)
            }
            val lss = ListSpeedSearch(stepTwoSearchSelect)
            val dp = DialogPanel("Files Containing Import:")
            stepTwoFileLink.isEnabled=false
            dp.add(stepTwoFileLink, BorderLayout.NORTH)
            val jbsp = JBScrollPane(lss.component)
            dp.add(jbsp)
            return dp
        }
    }
}
