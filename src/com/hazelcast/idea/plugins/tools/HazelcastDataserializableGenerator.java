package com.hazelcast.idea.plugins.tools;

import java.util.List;

import org.apache.commons.lang.WordUtils;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;

public class HazelcastDataserializableGenerator extends UtilityGeneration {
	public HazelcastDataserializableGenerator() {
		super("Generate Hazelcast Dataserializable write and read", "Select fields to generate:");
	}

	public void generate(final PsiClass psiClass, final List<PsiField> fields) {
		new WriteCommandAction.Simple(psiClass.getProject(), psiClass.getContainingFile()) {
			@Override
			protected void run() throws Throwable {
				generateWrite(psiClass, fields);
				generateRead(psiClass, fields);
			}
		}.execute();
	}


	private void generateWrite(PsiClass psiClass, List<PsiField> fields) {
		StringBuilder builder = new StringBuilder("@Override\n");
		builder.append("public void writeData(ObjectDataOutput out) throws IOException {\n");

		writeFields(psiClass, fields, builder);

		builder.append("}");
		setNewMethod(psiClass, builder.toString(), "writeData");
	}

	private void writeFields(PsiClass psiClass, List<PsiField> fields, StringBuilder builder) {
		final Project project = psiClass.getProject();
		final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
		final PsiType stringType = PsiType.getTypeByName("java.lang.String", project, scope);
		final PsiArrayType stringArrayType = stringType.createArrayType();
		for (PsiField field : fields) {
			final PsiType type = field.getType();
			final PsiType deepType = field.getType().getDeepComponentType();
			boolean isArray = type instanceof PsiArrayType;
			if(deepType.equals(PsiType.BOOLEAN) || deepType.equals(PsiType.BYTE) || deepType.equals(PsiType.CHAR) || deepType.equals(PsiType.SHORT)
				|| deepType.equals(PsiType.INT) || deepType.equals(PsiType.LONG) || deepType.equals(PsiType.FLOAT) || deepType.equals(PsiType.DOUBLE)) {
				final String typeName = WordUtils.capitalize(deepType.getPresentableText());
				writeField(builder, field.getName(), typeName, isArray);
			}
			else if(deepType instanceof PsiClassReferenceType) {
				if(type.equals(stringType)){
					writeField(builder, field.getName(), "UTF", false);
				} else if(type.equals(stringArrayType)){
					writeField(builder, field.getName(), "UTF", true);
				}
				else {
					writeField(builder, field.getName(), "Object", false);
				}
			}
		}
	}

	private void writeField(StringBuilder builder, String fieldName, String fieldType, boolean isArray) {
		builder.append("out.write")
			.append(fieldType)
			.append(isArray ? "Array" : "")
			.append("(")
			.append(fieldName)
			.append(");\n");

	}

	private void generateRead(PsiClass psiClass, List<PsiField> fields) {
		StringBuilder builder = new StringBuilder("@Override\n");
		builder.append("public void readData(ObjectDataInput in) throws IOException { \n");

		readFields(psiClass, fields, builder);

		builder.append("}");
		setNewMethod(psiClass, builder.toString(), "readData");
	}

	private void readFields(PsiClass psiClass, List<PsiField> fields, StringBuilder builder) {
		final Project project = psiClass.getProject();
		final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
		final PsiType stringType = PsiType.getTypeByName("java.lang.String", project, scope);
		final PsiArrayType stringArrayType = stringType.createArrayType();
		for (PsiField field : fields) {
			final PsiType type = field.getType();
			final PsiType deepType = field.getType().getDeepComponentType();
			boolean isArray = type instanceof PsiArrayType;
			if(deepType.equals(PsiType.BOOLEAN) || deepType.equals(PsiType.BYTE) || deepType.equals(PsiType.CHAR) || deepType.equals(PsiType.SHORT)
				|| deepType.equals(PsiType.INT) || deepType.equals(PsiType.LONG) || deepType.equals(PsiType.FLOAT) || deepType.equals(PsiType.DOUBLE)) {
				final String typeName = WordUtils.capitalize(deepType.getPresentableText());
				readField(builder, field.getName(), typeName, isArray);
			}
			else if(deepType instanceof PsiClassReferenceType) {
				if(type.equals(stringType)){
					readField(builder, field.getName(), "UTF", false);
				} else if(type.equals(stringArrayType)){
					readField(builder, field.getName(), "UTF", true);
				}
				else {
					readField(builder, field.getName(), "Object", false);
				}
			}
		}
	}

	private void readField(StringBuilder builder, String fieldName, String fieldType, boolean isArray) {
		builder.append("this.");
		builder.append(fieldName);
		builder.append(" = in.read");
		builder.append(fieldType);
		builder.append(isArray ? "Array" : "");
		builder.append("();\n");
	}
}
