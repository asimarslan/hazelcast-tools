package com.hazelcast.idea.plugins.toolcast;

import java.util.ArrayList;
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

		final List<PsiField> remaining = writeNativeFields(psiClass, fields, builder);
		writeObjects(psiClass, remaining, builder);

		builder.append("}");
		setNewMethod(psiClass, builder.toString(), "writePortable");
	}

	private List<PsiField> writeNativeFields(PsiClass psiClass, List<PsiField> fields, StringBuilder builder) {
		List<PsiField> remaining = new ArrayList<PsiField>();
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
				PsiClassReferenceType tt = (PsiClassReferenceType) deepType;
				if(type.equals(stringType)){
					writeField(builder, field.getName(), "UTF", false);
				} else if(type.equals(stringArrayType)){
					writeField(builder, field.getName(), "UTF", true);
				}
				else {
					remaining.add(field);
				}
			}
			else {
				remaining.add(field);
			}
		}
		return remaining;
	}

	private void writeObjects(PsiClass psiClass, List<PsiField> fields, StringBuilder builder) {
		boolean rawWritten = false;
		final Project project = psiClass.getProject();
		final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
		final PsiType dataserializableType = PsiType.getTypeByName("com.hazelcast.nio.serialization.DataSerializable", project, scope);
		for (PsiField field : fields) {
			final PsiType type = field.getType();
			final PsiType deepType = field.getType().getDeepComponentType();
			if(type instanceof PsiClassReferenceType){
				PsiClassReferenceType tt = (PsiClassReferenceType) deepType;
				if (dataserializableType.isAssignableFrom(tt)) {
					if(!rawWritten){
						rawWritten = true;
						builder.append("ObjectDataOutput rawDataOutput = out.getRawDataOutput();");
					}
					final String isNotNullText = "isNotNull" + WordUtils.capitalize(field.getName());
					builder.append("boolean ");
					builder.append(isNotNullText);
					builder.append(" = ");
					builder.append(field.getName());
					builder.append(" != null;\n");
					builder.append("if (");
					builder.append(isNotNullText);
					builder.append(") {\n");
					builder.append("    rawDataOutput.writeBoolean(");
					builder.append(isNotNullText);
					builder.append(");\n");
					builder.append("    ");
					builder.append(field.getName());
					builder.append(".writeData(rawDataOutput);\n");
					builder.append("} else {");
					builder.append("    rawDataOutput.writeBoolean(");
					builder.append(isNotNullText);
					builder.append(");\n");
					builder.append("}");
				}
				else {
					if(!rawWritten){
						rawWritten = true;
						builder.append("ObjectDataOutput rawDataOutput = out.getRawDataOutput();");
					}
					builder.append("rawDataOutput.writeObject(");
					builder.append(field.getName());
					builder.append(");\n");
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
		builder.append("public void readPortable(PortableReader in) throws IOException { \n");

		final List<PsiField> remaining = readNativePortableFields(psiClass, fields, builder);
		readObjects(psiClass, remaining, builder);

		builder.append("}");
		setNewMethod(psiClass, builder.toString(), "readPortable");
	}

	private List<PsiField> readNativePortableFields(PsiClass psiClass, List<PsiField> fields, StringBuilder builder) {
		List<PsiField> remaining = new ArrayList<PsiField>();
		final Project project = psiClass.getProject();
		final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
		final PsiType portableType = PsiType.getTypeByName("com.hazelcast.nio.serialization.Portable", project, scope);
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
				PsiClassReferenceType tt = (PsiClassReferenceType) deepType;
				if (portableType.isAssignableFrom(tt)) {
					if(isArray){
						builder.append("Portable[] ");
						builder.append(field.getName());
						builder.append(" = in.readPortableArray(\"");
						builder.append(field.getName());
						builder.append("\");\n");
						builder.append("this.");
						builder.append(field.getName());
						builder.append(" = new ");
						builder.append(tt.getClassName());
						builder.append("[");
						builder.append(field.getName());
						builder.append(".length];\n");
						builder.append("System.arraycopy(");
						builder.append(field.getName());
						builder.append(", 0, this.");
						builder.append(field.getName());
						builder.append(", 0, ");
						builder.append(field.getName());
						builder.append(".length);\n");
					} else {
						readField(builder, field.getName(), "Portable", isArray);
					}
				}
				else if(type.equals(stringType)){
					readField(builder, field.getName(), "UTF", false);
				} else if(type.equals(stringArrayType)){
					readField(builder, field.getName(), "UTF", true);
				}
				else {
					remaining.add(field);
				}
			}
			else {
				remaining.add(field);
			}
		}
		return remaining;
	}

	private void readObjects(PsiClass psiClass, List<PsiField> fields, StringBuilder builder) {
		boolean rawWritten = false;
		final Project project = psiClass.getProject();
		final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
		final PsiType dataserializableType = PsiType.getTypeByName("com.hazelcast.nio.serialization.DataSerializable", project, scope);
		for (PsiField field : fields) {
			final PsiType type = field.getType();
			final PsiType deepType = field.getType().getDeepComponentType();
			if(type instanceof PsiClassReferenceType){
				PsiClassReferenceType tt = (PsiClassReferenceType) deepType;
				if (dataserializableType.isAssignableFrom(tt)) {
					if(!rawWritten){
						rawWritten = true;
						builder.append("ObjectDataInput rawDataInput = in.getRawDataInput();");
					}
					final String isNotNullText = "isNotNull" + WordUtils.capitalize(field.getName());
					builder.append("boolean ");
					builder.append(isNotNullText);
					builder.append(" = ");
					builder.append("rawDataInput.readBoolean();\n");
					builder.append("if (");
					builder.append(isNotNullText);
					builder.append(") {\n");
					builder.append("    ");
					builder.append(tt.getClassName());
					builder.append(" ");
					builder.append(field.getName());
					builder.append(" = new ");
					builder.append(tt.getClassName());
					builder.append("();\n");
					builder.append(field.getName());
					builder.append(".readData(rawDataInput);\n");
					builder.append("    this.");
					builder.append(field.getName());
					builder.append(" = ");
					builder.append(field.getName());
					builder.append(";\n");
					builder.append("}\n");
				}
				else {
					if(!rawWritten){
						rawWritten = true;
						builder.append("ObjectDataInput rawDataInput = in.getRawDataInput();");
					}
					builder.append("this.");
					builder.append(field.getName());
					builder.append(" = rawDataInput.readObject();\n");
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
		builder.append("(\"");
		builder.append(fieldName);
		builder.append("\");\n");
	}
}
