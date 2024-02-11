package com.josephggd.importindex

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.LanguageTextField
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.text.Document


internal class FileToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = FileToolWindowContent(toolWindow, project)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
    private class FileToolWindowContent(toolWindow: ToolWindow, project:Project) : FileLogic(project) {
        val mainPanel = JPanel()
        var importStatements = emptyList<Import>()
        var imports = emptyList<String>()
        var selectedImport:String? = null
        var importingFiles = emptyList<String>()
        var selectedFile:String?="no file selected"
        init {
            refreshImportStatements()
            mainPanel.layout = BorderLayout(0,0)
            mainPanel.border = BorderFactory.createEmptyBorder(20,20,20,20)
            mainPanel.add(createControlPanel(toolWindow), BorderLayout.NORTH)

            val importSearch = createSearch(
                "loading",
                imports
            ) {
                importSearchCallback(it)
            }
            mainPanel.add(importSearch, BorderLayout.WEST)

            val fileSearch = createSearch(
                "select an import",
                importingFiles
            ) {
                fileSearchCallback(it)
            }
            mainPanel.add(fileSearch, BorderLayout.CENTER)

            val fileView = JLabel(selectedFile)
            mainPanel.add(fileView, BorderLayout.EAST)
        }
        fun importSearchCallback(inputEvent: ListSelectionEvent) {
            println("inputEvent.firstIndex:${inputEvent.firstIndex}")
            println("inputEvent.lastIndex:${inputEvent.lastIndex}")
            val selectedImport = importStatements.sortedBy { it.imported }[inputEvent.lastIndex].imported
            println("selectedImport:"+selectedImport)
            println("!!:"+importStatements[inputEvent.lastIndex])
            mainPanel.remove(2)

            val newFiles = importStatements
                    .asSequence()
                    .filter { it.imported==selectedImport }
                    .map { it.importing }
                    .toSet()
                    .toList()
                    .sorted()
            println("newFiles:"+newFiles)

            val fileSearch = createSearch(
                "select an import",
                newFiles
            ) {
                fileSearchCallback(it)
            }
            mainPanel.add(fileSearch, BorderLayout.CENTER)
            mainPanel.repaint()
        }

//        fun sdsd(){
//            val sdsd = PsiManager.getInstance(project).findViewProvider()
//            val psiFile: PsiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE)
//            val element = psiFile.findElementAt(offset)
//            val containingMethod = PsiTreeUtil.getParentOfType(
//                element,
//                PsiMethod::class.java
//            )
//            val containingClass = containingMethod!!.containingClass!!
//        }

//        fun navToFile(){
//            val psiFile = PsiDocumentManager.getInstance(project)
//                .getPsiFile(editor.getDocument())
//            val element: PsiElement =
//                psiFile.findElementAt(editor.getCaretModel().getOffset())
//            val code =
//                JavaCodeFragmentFactory.getInstance(project)
//                    .createExpressionCodeFragment("", element, null, true)
//
//            val document: Document? =
//                PsiDocumentManager.getInstance(project).getDocument(code)
//        }
        fun fileSearchCallback(inputEvent: ListSelectionEvent) {
            val selectedFile = importStatements.sortedBy { it.importing }[inputEvent.lastIndex].importing
            try {
                mainPanel.remove(3)
            } catch (e:Exception) {

            }

            val lang = Language.findLanguageByID("Java")
            val ltf = JLabel( selectedFile )
            mainPanel.add(ltf, BorderLayout.EAST)
            mainPanel.repaint()
        }
        fun refreshImportStatements(){
            importStatements = getProjectLevelImports()
            imports = importStatements
                .map { it.imported }
                .toSet()
                .toList()
                .sorted()
        }
        fun selectImportToRefreshFiles(anImport:String){
            importingFiles = importStatements
                .asSequence()
                .filter { it.imported==anImport }
                .map { it.importing }
                .toSet()
                .toList()
                .sorted()
            println("SELECT AN IMPORT")
            println("importingFiles:$importingFiles")
        }
        fun selectFileToRefreshFile(aFile:String){
            selectedFile = importStatements
                .map { it.importing }
                .find { it==aFile }
            println("SELECT A FILE")
            println("importingFiles:$selectedFile")
        }
        fun createSearch(loading:String, list:List<String>, listener:(lse:ListSelectionEvent)->Unit) : JBScrollPane {
            val jbl = JBList(list)
            jbl.setEmptyText(loading)
            jbl.selectionMode=ListSelectionModel.SINGLE_SELECTION
            jbl.addListSelectionListener {
                listener(it)
            }
            val lss = ListSpeedSearch(jbl)
            val jbsp = JBScrollPane(lss.component)
            return jbsp
        }
        fun createControlPanel(toolWindow: ToolWindow):JPanel {
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


//class CustomLanguageTextField(document:Document, project: Project) : LanguageTextField(document, project,
//    JavaFileType.INSTANCE.toString()
//) {
//
//    override fun createEditor(): EditorEx {
//        val editor = super.createEditor()
//        editor.setVerticalScrollbarVisible(true)
//        editor.setHorizontalScrollbarVisible(true)
//
//        val settings = editor.settings
//        settings.isLineNumbersShown = true
//        settings.isAutoCodeFoldingEnabled = true
//        settings.isFoldingOutlineShown = true
//        settings.isAllowSingleLogicalLineFolding = true
//        settings.isRightMarginShown=true
//        return editor
//    }
//}
