package com.github.nizienko.propertiesSwitcher

import com.github.nizienko.propertiesSwitcher.fileType.PropertiesTemplateFileType
import com.github.nizienko.propertiesSwitcher.statusBar.PropertiesStatusBarWidgetFactory
import com.github.nizienko.propertiesSwitcher.statusBar.SwitcherWidget.Companion.ID
import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CountDownLatch


internal class PropertySwitcherStarter : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.switcher().reload()
    }
}

internal class PropertySwitcherService(private val project: Project) {
    private val switchableFiles: MutableList<SwitchablePropertyFile> = Collections.synchronizedList(mutableListOf())

    fun reloadFile(file: VirtualFile) {
        if (file.fileType == PropertiesTemplateFileType.INSTANCE) {
            parseJsonFile(file)?.let { template ->
                val propertyFile = findOrCreateFileIfNotExist(template)
                switchableFiles.firstOrNull { it.propertyTemplate.propertyFile == template.propertyFile }?.let {
                    switchableFiles.remove(it)
                }
                switchableFiles.add(SwitchablePropertyFile(template, propertyFile))
            }
            updateWidget()
        }
    }

    fun reload() {
        switchableFiles.clear()
        PropertiesTemplateFileType.findAllFiles(project).forEach { file ->
            reloadFile(file)
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
                val latch = CountDownLatch(1)
                ApplicationManager.getApplication().invokeLater {
                    ApplicationManager.getApplication().runWriteAction {
                        val newFile = parentDir.createChildData(this, filePath.substringAfterLast("/"))
                        VfsUtil.saveText(newFile, content)
                        latch.countDown()
                    }
                }
                latch.await()
            }
            return fileSystem.findFileByPath(baseDir.path + "/" + filePath)
                ?: throw IllegalStateException("Failed to create Property file for ${data.propertyFile}")
        }
    }

    fun getSwitchableFiles() = switchableFiles.toList()

    private fun parseJsonFile(file: VirtualFile): PropertiesTemplate? {
        val content = VfsUtilCore.loadText(file)
        val inputStream = file.inputStream
        val reader = InputStreamReader(inputStream)

        val gson = Gson()
        return try {
            gson.fromJson(content, PropertiesTemplate::class.java)
        } catch (e: Exception) {
            null
        } finally {
            reader.close()
            inputStream.close()
        }
    }

    fun getStatusBarValues(): String {
        return getSwitchableFiles().flatMap { file ->
            val params = file.readProperties()
            file.propertyTemplate.properties.filter { it.showInStatusBar ?: false }
                .mapNotNull { params[it.name] }

        }.joinToString(" ") { it }
    }
     fun updateWidget() {
         project.service<StatusBarWidgetsManager>().updateWidget(PropertiesStatusBarWidgetFactory::class.java)
         val statusBar = WindowManager.getInstance().getStatusBar(project)
         statusBar?.updateWidget(ID)
     }
}

internal class SwitchablePropertyFile(val propertyTemplate: PropertiesTemplate, private val propertyFile: VirtualFile) {
    init {
        addMissingProperties()
    }

    private fun addMissingProperties() {
        val existedProperties = readProperties().toMutableMap()
        propertyTemplate.properties.forEach {
            if (existedProperties.keys.contains(it.name).not()) {
                existedProperties[it.name] = it.options.last()
            }
        }
        saveProperties(existedProperties)
    }
    private fun notifyChanges() {
        ProjectLocator.getInstance().guessProjectForFile(propertyFile)?.switcher()?.updateWidget()
    }

    internal fun readProperties(): Map<String, String> {
        return String(propertyFile.contentsToByteArray(), StandardCharsets.UTF_8).split("\n")
            .filter { it.contains("=") && it.length > 3 }.associate {
                val key = it.substringBefore("=")
                val value = it.substringAfter("=", "")
                key to value
            }
    }

    private fun saveProperties(newProperties: Map<String, String>) {
        val content = buildString {
            newProperties.forEach { (k, v) ->
                append(k)
                append("=")
                append(v)
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
        saveProperties(readProperties().toMutableMap().apply { put(name, value) })
    }
}

internal data class PropertiesTemplate(
    val propertyFile: String,
    val properties: List<Prop>
)

internal data class Prop(
    val name: String,
    val options: List<String>,
    val showInStatusBar: Boolean?
)

internal fun Project.switcher() = service<PropertySwitcherService>()
