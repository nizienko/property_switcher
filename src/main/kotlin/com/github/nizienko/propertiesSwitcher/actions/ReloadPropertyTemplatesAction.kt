package com.github.nizienko.propertiesSwitcher.actions

import com.github.nizienko.propertiesSwitcher.PropertySwitcherService
import com.github.nizienko.propertiesSwitcher.fileType.PropertiesTemplateFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager


internal class ReloadPropertyTemplatesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(LangDataKeys.EDITOR) ?: return
        FileDocumentManager.getInstance().saveDocument(editor.document)

        val service = project.service<PropertySwitcherService>()
        service.reload()
    }

    override fun update(event: AnActionEvent) {
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val presentation = event.presentation
        presentation.isEnabled = virtualFile != null && virtualFile.fileType == PropertiesTemplateFileType.INSTANCE
    }
}