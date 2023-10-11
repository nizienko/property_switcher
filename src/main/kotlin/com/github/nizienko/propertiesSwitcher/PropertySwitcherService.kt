package com.github.nizienko.propertiesSwitcher

import com.github.nizienko.propertiesSwitcher.fileType.PropertiesTemplateFileType
import com.github.nizienko.propertiesSwitcher.statusBar.PropertiesStatusBarWidgetFactory
import com.github.nizienko.propertiesSwitcher.statusBar.SwitcherWidget.Companion.ID
import com.google.gson.Gson
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.*
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.text.Regex.Companion.escapeReplacement


internal class PropertySwitcherStarter : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.switcher().reload()
    }
}

@Service(Service.Level.PROJECT)
internal class PropertySwitcherService(private val project: Project) {
    private val switchableFiles: MutableList<SwitchablePropertyFile> = Collections.synchronizedList(mutableListOf())

    private fun loadFile(file: VirtualFile) {
        if (file.fileType == PropertiesTemplateFileType.INSTANCE) {
            parseJsonFile(file)?.let { template ->
                val propertyFile = findOrCreateFileIfNotExist(template)
                switchableFiles.firstOrNull { it.propertyFile == propertyFile }?.addTemplate(template)
                    ?: switchableFiles.add(SwitchablePropertyFile(template, propertyFile))
            }
            updateWidget()
        }
    }

    fun reload() {
        switchableFiles.clear()
        PropertiesTemplateFileType.findAllFiles(project).forEach { file ->
            loadFile(file)
        }
    }

    fun isAvailable(): Boolean = switchableFiles.isNotEmpty()

    private fun findOrCreateFileIfNotExist(data: PropertiesTemplate): VirtualFile {
        val fileSystem = LocalFileSystem.getInstance()
        val baseDir = project.guessProjectDir() ?: throw IllegalStateException("Can't find project folder")
        val filePath = data.propertyFile
        val file = fileSystem.findFileByPath(baseDir.path + "/" + filePath)
        if (file != null) {
            return file
        } else {
            val parentDirPath = if (filePath.contains("/")) {
                baseDir.path + "/" + filePath.substringBeforeLast("/")
            } else {
                baseDir.path
            }
            val parentDir = fileSystem.refreshAndFindFileByPath(parentDirPath)

            if (parentDir != null) {
                val content = buildString {
                    data.properties.forEach {
                        append(it.name)
                        append("=")
                        append(it.options.last())
                        append("\n")
                    }
                }
                if (ApplicationManager.getApplication().isDispatchThread) {
                    ApplicationManager.getApplication().runWriteAction {
                        val newFile = parentDir.createChildData(this, filePath.substringAfterLast("/"))
                        VfsUtil.saveText(newFile, content)
                    }
                } else {
                    val latch = CountDownLatch(1)
                    ApplicationManager.getApplication().invokeLater {
                        ApplicationManager.getApplication().runWriteAction {
                            try {
                                val newFile = parentDir.createChildData(this, filePath.substringAfterLast("/"))
                                VfsUtil.saveText(newFile, content)
                            } finally {
                                latch.countDown()
                            }
                        }
                    }
                    latch.await()
                }
            }
            return fileSystem.findFileByPath(baseDir.path + "/" + filePath)
                ?: throw IllegalStateException("Failed to create Property file for ${data.propertyFile}")
        }
    }

    fun getSwitchableFiles() = switchableFiles.toList()

    @Suppress("SENSELESS_COMPARISON")
    private fun parseJsonFile(file: VirtualFile): PropertiesTemplate? {
        val content = VfsUtilCore.loadText(file)
        val inputStream = file.inputStream
        val reader = InputStreamReader(inputStream)

        val gson = Gson()
        return try {
            gson.fromJson(content, PropertiesTemplate::class.java).apply {
                require(propertyFile != null) {
                    "propertyFile value is missing"
                }
                require(properties != null) {
                    "properties are missing"
                }
                properties.forEach {
                    require(it.name != null) {
                        "name is missing"
                    }
                    require(it.options != null) {
                        "options are missing for ${it.name} property"
                    }
                    require(it.options.isNotEmpty()) {
                        "${it.name} options are empty"
                    }
                }
            }
        } catch (e: Exception) {
            Notifications.Bus.notify(
                Notification(
                    "Property Switcher Notification Group",
                    "Failed to load ${file.name}:\n${e.message}",
                    NotificationType.ERROR
                ), project
            )
            null
        } finally {
            reader.close()
            inputStream.close()
        }
    }

    fun getStatusBarToolTip(): String {
        // use html for multiline tooltip
        return getSwitchableFiles().flatMap { file ->
            val params = file.readProperties()
            file.properties.map {
                val n = it.name.replace("&", "&amp;").replace("<", "&lt;")
                val v = params[it.name].toString().replace("&", "&amp;").replace("<", "&lt;")
                    .removeNewLinesForPropertyFile()
                // use <pre> for values to not lost spaces/line feeds.
                // use bold for property names to not mix with multiline values.
                "<b>$n: </b><pre>$v</pre></br>"
            }
        }.joinToString("", "<html>", "</html>") { it }
    }

    fun getStatusBarLabel(): String {
        return getSwitchableFiles().flatMap { file ->
            val params = file.readProperties()
            file.properties.filter { it.showInStatusBar ?: false }.mapNotNull { params[it.name] }

        }.joinToString(" ") { it }
    }

    fun updateWidget() {
        project.service<StatusBarWidgetsManager>().updateWidget(PropertiesStatusBarWidgetFactory::class.java)
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        statusBar?.updateWidget(ID)
    }
}

internal class SwitchablePropertyFile(
    propertyTemplates: PropertiesTemplate,
    val propertyFile: VirtualFile
) {
    private val templates = mutableListOf<PropertiesTemplate>()

    init {
        addTemplate(propertyTemplates)
    }

    fun addTemplate(template: PropertiesTemplate) {
        templates.add(template)
        addMissingProperties(template)
    }

    val properties: List<Prop>
        get() = templates.flatMap { it.properties }

    private fun addMissingProperties(propertyTemplate: PropertiesTemplate) {
        val existedProperties = readProperties()
        val propertiesToAdd = mutableMapOf<String, String>()
        propertyTemplate.properties.forEach {
            if (existedProperties.keys.contains(it.name).not()) {
                propertiesToAdd[it.name] = it.options.first()
            }
        }
        addProperties(propertiesToAdd)
    }

    private fun notifyChanges() {
        ProjectLocator.getInstance().guessProjectForFile(propertyFile)?.switcher()?.updateWidget()
    }

    internal fun readProperties(): Map<String, String> {
        val properties = Properties().apply { load(propertyFile.inputStream) }
        return properties.map { it.key as String to it.value as String }.toMap()
    }

    private fun addProperties(newProperties: Map<String, String>) {
        val content = buildString {
            val origin = propertyFile.readText()
            append(origin)
            if (origin.endsWith("\n").not()) append("\n")
            newProperties.forEach { (k, v) ->
                append(k)
                append("=")
                append(v.setNewLinesForPropertyFile())
                append("\n")
            }
        }
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                VfsUtil.saveText(propertyFile, content)
                notifyChanges()
            }
        }

    }

    fun updateParameter(name: String, value: String) {
        val documentManager = FileDocumentManager.getInstance()
        val document = documentManager.getDocument(propertyFile) ?: return
        val content = document.text.toOneLine()

        val currentValue = Regex(" *($name) *[=: ] *(.*)").find(content)?.groupValues?.get(2)

        val currentComment = currentValue?.takeIf {
            it.contains("#")
        }?.substringAfter("#")

        val newValue = currentComment?.let { "${value.setNewLinesForPropertyFile()} #$currentComment" }
            ?: value.setNewLinesForPropertyFile()

        val updatedContent = if (currentValue != null) {
            content.replace((" *($name) *([=: ]) *(.*)").toRegex(), "$1$2${escapeReplacement(newValue)}")
                .toMultiLine()
        } else {
            if (content.contains(name)) {
                content.replace(name, "$name=${escapeReplacement(newValue)}")
            } else {
                buildString {
                    append(content)
                    append("\n")
                    append("$name=${escapeReplacement(newValue)}")
                }
            }
        }

        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                document.setText(updatedContent)
                documentManager.saveDocument(document)
                notifyChanges()
            }
        }
    }
}

internal data class PropertiesTemplate(
    val propertyFile: String, val properties: List<Prop>
)

internal data class Prop(
    val name: String, val options: List<String>, val showInStatusBar: Boolean?
)

internal fun Project.switcher() = service<PropertySwitcherService>()

internal const val MULTILINE_PLACEHOLDER = "%MULTILINE_PLACEHOLDER%"
internal const val MULTILINE_CHAR = "\\\n"

internal fun String.toOneLine(): String = replace(MULTILINE_CHAR, MULTILINE_PLACEHOLDER)
internal fun String.toMultiLine(): String = replace(MULTILINE_PLACEHOLDER, MULTILINE_CHAR)
internal fun String.setNewLinesForPropertyFile(): String = replace("\n", MULTILINE_CHAR)
internal fun String.removeNewLinesForPropertyFile(): String = replace(MULTILINE_CHAR, "\n")
internal fun String.removeComments(): String = substringBefore("#").trim()