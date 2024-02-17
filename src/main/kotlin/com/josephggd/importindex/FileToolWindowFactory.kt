package com.josephggd.importindex

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiJavaFile
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent


internal class FileToolWindowFactory : ToolWindowFactory, DumbAware {
    private enum class Error(private val str:String) {
        REFRESH_FAILED("REFRESH FAILED"),
        IMPORT_CALLBACK_FAILED("IMPORT CALLBACK FAILED"),
        FILE_CALLBACK_FAILED("FILE CALLBACK FAILED"),
        BAD_PKG_NAME("BAD PKG NAME");
    }
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
        val stepTwoFileLink = HyperlinkLabel("FILE")
        val stepThreeFileView = LanguageTextField(JavaLanguage.INSTANCE, project, "")
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
            mainPanel.add(createFileView(), BorderLayout.EAST)
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
                    logger.warn(Error.BAD_PKG_NAME.toString())
                }
            }
            return empty.joinToString (".")
        }
        fun createFileView():JBScrollPane{
            val cm = EditorColorsManager.getInstance()
            stepThreeFileView.background = cm.schemeForCurrentUITheme.defaultBackground
            stepThreeFileView.setPreferredWidth(600)
            stepThreeFileView.setOneLineMode(false)
            stepThreeFileView.fileType=JavaFileType.INSTANCE
            return JBScrollPane(stepThreeFileView.component)
        }
        fun updateFileView(psiFile:PsiJavaFile){
            stepThreeFileView.setDocument(PsiDocumentManager.getInstance(project).getDocument(psiFile))
        }

        fun importCallback(lse:ListSelectionEvent) {
            try {
                val index = getSelectedIndex(lse)
                if (!lse.valueIsAdjusting) {
                    stepTwoFileLink.isEnabled=false
                    stepTwoFileLink.setHyperlinkText("...")
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
            } catch (e:Exception) {
                logger.warn(Error.IMPORT_CALLBACK_FAILED.toString())
            }
        }
        fun fileCallback(lse:ListSelectionEvent){
            try {
                if (!lse.valueIsAdjusting) {
                    val index = getSelectedIndex(lse)
                    val newF:PsiFileExtender = stepTwoSearchSelect.model.getElementAt(index)
                    stepTwoFileLink.isEnabled=true
                    stepTwoFileLink.setHyperlinkText(newF.psiFile.name)
                    updateFileView(newF.psiFile)
                    stepTwoFileLink.addHyperlinkListener {
                        newF.navigate()
                        newF.psiFile.containingDirectory.navigate(true)
                    }
                }
            } catch (e:Exception) {
                logger.warn(Error.FILE_CALLBACK_FAILED.toString())
            }
        }
        fun refreshImportStatements(){
            try {
                importToFileMap = getImportsAsMap()
                stepOneSearchSelect.setListData(importToFileMap.keys.sorted().toTypedArray())
                importingFiles= emptyList()
                selectedFileName=""
            } catch (e:Exception) {
                logger.warn(Error.REFRESH_FAILED.toString())
            }
        }
        fun createImportSearch() : DialogPanel {
            val dp = DialogPanel("Project Imports:")
            stepOneSearchSelect.setEmptyText("Loading")
            stepOneSearchSelect.setListData(importToFileMap.keys.sorted().toTypedArray())
            stepOneSearchSelect.selectionMode=ListSelectionModel.SINGLE_SELECTION
            stepOneSearchSelect.addListSelectionListener {
                try {
                    importCallback(it)
                } catch (e:Exception) {
                    logger.warn(Error.IMPORT_CALLBACK_FAILED.toString())
                }
            }
            val jbsp = JBScrollPane(stepOneSearchSelect)
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
                try {
                    fileCallback(it)
                } catch (e:Exception) {
                    logger.warn(Error.IMPORT_CALLBACK_FAILED.toString())
                }
            }
            val dp = DialogPanel("Files Containing Import:")
            stepTwoFileLink.isEnabled=false
            stepTwoFileLink.setHyperlinkText("...")
            dp.add(stepTwoFileLink, BorderLayout.NORTH)
            val jbsp = JBScrollPane(stepTwoSearchSelect)
            dp.add(jbsp)
            return dp
        }
    }
}
