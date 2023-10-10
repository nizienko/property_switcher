package com.github.nizienko.propertiesSwitcher.statusBar

import com.github.nizienko.propertiesSwitcher.switcher
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import kotlinx.coroutines.runBlocking
import java.awt.Component
import java.awt.event.MouseEvent

internal class PropertiesStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "propertySwitcher"

    override fun getDisplayName(): String = "Property Switcher"

    override fun isAvailable(project: Project): Boolean {
        return project.switcher().isAvailable()
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return SwitcherWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget)
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}

internal class SwitcherWidget(private val project: Project) : StatusBarWidget {
    companion object {
        const val ID = "Switcher Widget"
    }

    override fun dispose() {
        Disposer.dispose(this)
    }

    override fun ID(): String = ID
    override fun install(statusBar: StatusBar) {
    }

    override fun getPresentation() = object : StatusBarWidget.TextPresentation {
        override fun getTooltipText(): String = project.switcher().getStatusBarToolTip()

        override fun getClickConsumer(): Consumer<MouseEvent>? {
            return Consumer {
                runBlocking {
                    val dataContext = DataManager.getInstance().getDataContext(it.component)
                    val event = AnActionEvent.createFromInputEvent(it, "", null, dataContext)
                    ActionManager.getInstance()
                        .getAction("com.github.nizienko.propertiesSwitcher.actions.SwitchPropertiesAction")
                        .actionPerformed(event)
                }
            }
        }

        override fun getText(): String {
            return project.switcher().getStatusBarLabel().let {
                if (it.isEmpty()) "PropSwitcher"
                else if (it.length > 48) it.substring(0, 45) + "..."
                else it
            }
        }

        override fun getAlignment(): Float {
            return Component.CENTER_ALIGNMENT
        }
    }
}