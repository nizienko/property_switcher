package com.github.nizienko.propertiesSwitcher.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import icons.MyIcons

class CreateNewPropSwitchAction : CreateFileFromTemplateAction("Property Switcher", "Create new property switcher file", MyIcons.PS) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New Property Switcher")
            .addKind("JSON", MyIcons.PS, "local.propswitch")
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return "New Property Switcher"
    }
}