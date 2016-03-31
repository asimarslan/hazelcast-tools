package com.hazelcast.idea.plugins.tools

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

abstract class UtilityGeneration(text: String, private val dialogTitle: String) : AnAction(text) {

    override fun actionPerformed(e: AnActionEvent) {
        val psiClass = getPsiClassFromContext(e)
        val dlg = GenerateDialog(psiClass!!, dialogTitle)
        dlg.show()
        if (dlg.isOK) {
            generate(psiClass, dlg.fields)
        }
    }

    //    private fun findLanguageLevel(e: AnActionEvent): LanguageLevel {
    //        val module = DataKeys.MODULE.getData(e.dataContext)
    //        var languageLevel = LanguageLevel.JDK_1_6
    //
    //        val project = e.project
    //        if (project != null) {
    //            languageLevel = LanguageLevelProjectExtension.getInstance(project).languageLevel
    //        }
    //
    //        if (module != null) {
    //            val moduleLanguageLevel = LanguageLevelModuleExtensionImpl.getInstance(module).languageLevel
    //            languageLevel = moduleLanguageLevel ?: languageLevel
    //        }
    //
    //        return languageLevel
    //    }

    abstract fun generate(psiClass: PsiClass, fields: List<PsiField>)

    protected fun setNewMethod(psiClass: PsiClass, newMethodBody: String, methodName: String) {
        val elementFactory = JavaPsiFacade.getElementFactory(psiClass.project)
        val newEqualsMethod = elementFactory.createMethodFromText(newMethodBody, psiClass)
        val method = addOrReplaceMethod(psiClass, newEqualsMethod, methodName)
        JavaCodeStyleManager.getInstance(psiClass.project).shortenClassReferences(method)
    }

    protected fun addOrReplaceMethod(psiClass: PsiClass, newEqualsMethod: PsiMethod, methodName: String): PsiElement {
        val existingEqualsMethod = findMethod(psiClass, methodName)
        return if (existingEqualsMethod != null) existingEqualsMethod.replace(newEqualsMethod) else psiClass.add(newEqualsMethod)
    }

    protected fun findMethod(psiClass: PsiClass, methodName: String): PsiMethod? {
        val allMethods = psiClass.allMethods
        for (method in allMethods) {
            if (psiClass.name == method.containingClass!!.name && methodName == method.name) {
                return method
            }
        }
        return null
    }

    override fun update(e: AnActionEvent) {
        val psiClass = getPsiClassFromContext(e)
        e.presentation.isEnabled = psiClass != null
        e.presentation.icon = IconConstants.HZ_ACTION
    }

    private fun getPsiClassFromContext(e: AnActionEvent): PsiClass? {
        val psiFile = e.getData(LangDataKeys.PSI_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (psiFile == null || editor == null) {
            return null
        }
        val offset = editor.caretModel.offset
        val elementAt = psiFile.findElementAt(offset)
        return PsiTreeUtil.getParentOfType(elementAt, PsiClass::class.java)
    }
}
