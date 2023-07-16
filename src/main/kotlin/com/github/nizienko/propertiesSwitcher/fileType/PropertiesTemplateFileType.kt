package com.github.nizienko.propertiesSwitcher.fileType

import com.intellij.json.JsonLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import icons.MyIcons
import javax.swing.Icon

internal class PropertiesTemplateFileType : LanguageFileType(JsonLanguage.INSTANCE) {

    companion object {
        @JvmStatic
        val INSTANCE = PropertiesTemplateFileType()

        fun findAllFiles(project: Project): List<VirtualFile> {
            return ApplicationManager.getApplication().runReadAction<List<VirtualFile>> {
                FileTypeIndex.getFiles(
                    INSTANCE,
                    GlobalSearchScope.allScope(project)
                ).toList()
            }
        }
    }

    override fun getName(): String = "Properties Template File Type"

    override fun getDescription(): String = "Properties template"

    override fun getDefaultExtension(): String = "propswitch"

    override fun getIcon(): Icon = MyIcons.PS
}