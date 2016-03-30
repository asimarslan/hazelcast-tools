package com.hazelcast.idea.plugins.tools

import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel

class GenerateDialog(psiClass: PsiClass, dialogTitle: String) : DialogWrapper(psiClass.project) {
    private val myComponent: LabeledComponent<JPanel>
    private val fieldList: JBList

    init {
        title = dialogTitle

        val myFields = CollectionListModel(*psiClass.allFields)
        fieldList = JBList(myFields)
        fieldList.cellRenderer = DefaultPsiElementCellRenderer()
        val decorator = ToolbarDecorator.createDecorator(fieldList)
        decorator.disableAddAction()
        val panel = decorator.createPanel()
        myComponent = LabeledComponent.create(panel, dialogTitle + " (Warning: existing method(s) will be replaced):")


        init()
    }

    override fun createCenterPanel(): JComponent? {
        return myComponent
    }

    val fields: List<PsiField>
        get() = selectedValuesList

    val selectedValuesList: List<PsiField>
        get() {
            val sm = fieldList.selectionModel
            val dm = fieldList.model

            val iMin = sm.minSelectionIndex
            val iMax = sm.maxSelectionIndex

            if (iMin < 0 || iMax < 0) {
                return emptyList()
            }

            val selectedItems = ArrayList<PsiField>()
            for (i in iMin..iMax) {
                if (sm.isSelectedIndex(i)) {
                    selectedItems.add(dm.getElementAt(i) as PsiField)
                }
            }
            return selectedItems
        }

}