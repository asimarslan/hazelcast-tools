package com.hazelcast.idea.plugins.tools;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.Nullable;

import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;

public class GenerateDialog extends DialogWrapper {
//	private CollectionListModel<PsiField> myFields;
	private final LabeledComponent<JPanel> myComponent;
	private final JBList fieldList;

	public GenerateDialog(PsiClass psiClass, String dialogTitle) {
		super(psiClass.getProject());
		setTitle(dialogTitle);

		CollectionListModel<PsiField> myFields = new CollectionListModel<PsiField>(psiClass.getAllFields());
		fieldList = new JBList(myFields);
		fieldList.setCellRenderer(new DefaultPsiElementCellRenderer());
		ToolbarDecorator decorator = ToolbarDecorator.createDecorator(fieldList);
		decorator.disableAddAction();
		JPanel panel = decorator.createPanel();
		myComponent = LabeledComponent.create(panel, dialogTitle + " (Warning: existing method(s) will be replaced):");


		init();
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return myComponent;
	}

	public List<PsiField> getFields() {
		return fieldList.getSelectedValuesList();
	}
}