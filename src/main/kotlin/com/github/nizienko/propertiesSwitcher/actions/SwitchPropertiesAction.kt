package com.github.nizienko.propertiesSwitcher.actions

import com.github.nizienko.propertiesSwitcher.Prop
import com.github.nizienko.propertiesSwitcher.SwitchablePropertyFile
import com.github.nizienko.propertiesSwitcher.switcher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep

internal class SwitchPropertiesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val service = e.project?.switcher() ?: return
        val files = service.getSwitchableFiles()
        val popupStep = createSwitchableFilesPopup(files)
        val popup = JBPopupFactory.getInstance().createListPopup(popupStep)
        popup.showInBestPositionFor(e.dataContext)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
    }
}

private fun createSwitchableFilesPopup(
    options: List<SwitchablePropertyFile>
) = if (options.size != 1) {
    object : BaseListPopupStep<SwitchablePropertyFile>("File", options) {
        override fun isSpeedSearchEnabled(): Boolean {
            return true
        }

        override fun onChosen(selectedValue: SwitchablePropertyFile, finalChoice: Boolean): PopupStep<*>? {
            return createPropsPopup(selectedValue)
        }

        override fun hasSubstep(selectedValue: SwitchablePropertyFile): Boolean {
            return true
        }

        override fun getTextFor(value: SwitchablePropertyFile): String {
            return value.propertyTemplate.propertyFile
        }
    }
} else {
    createPropsPopup(options.first())
}


private fun createPropsPopup(
    chosenFile: SwitchablePropertyFile
) = object : BaseListPopupStep<Prop>("Property", chosenFile.propertyTemplate.properties) {
    override fun isSpeedSearchEnabled(): Boolean {
        return true
    }

    override fun onChosen(selectedValue: Prop, finalChoice: Boolean): PopupStep<*>? {
        return createValuePopup(chosenFile, selectedValue)
    }

    override fun hasSubstep(selectedValue: Prop): Boolean {
        return true
    }

    override fun getTextFor(value: Prop): String {
        return value.name
    }
}

private fun createValuePopup(
    chosenFile: SwitchablePropertyFile,
    chosenProp: Prop
) = object : BaseListPopupStep<String>("Value", chosenProp.options) {
    override fun isSpeedSearchEnabled(): Boolean {
        return true
    }

    override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
        if (finalChoice) {
            chosenFile.updateParameter(chosenProp.name, selectedValue)
        }
        return FINAL_CHOICE
    }

    override fun hasSubstep(selectedValue: String): Boolean {
        return false
    }

    override fun getTextFor(value: String): String {
        return value
    }
}