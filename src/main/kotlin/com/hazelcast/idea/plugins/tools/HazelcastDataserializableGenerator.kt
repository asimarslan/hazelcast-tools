package com.hazelcast.idea.plugins.tools

import org.apache.commons.lang.WordUtils

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiType
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope

class HazelcastDataserializableGenerator : UtilityGeneration("Generate Hazelcast Dataserializable write and read", "Select fields to generate:") {

    override fun generate(psiClass: PsiClass, fields: List<PsiField>) {
        object : WriteCommandAction.Simple<Unit>(psiClass.project, psiClass.containingFile) {
            override fun run() {
                generateWrite(psiClass, fields)
                generateRead(psiClass, fields)
            }
        }.execute()
    }


    private fun generateWrite(psiClass: PsiClass, fields: List<PsiField>) {
        val builder = StringBuilder("@Override\n")
        builder.append("public void writeData(ObjectDataOutput out) throws IOException {\n")
        writeFields(psiClass, fields, builder)
        builder.append("}")
        setNewMethod(psiClass, builder.toString(), "writeData")
    }

    private fun writeFields(psiClass: PsiClass, fields: List<PsiField>, builder: StringBuilder) {
        val project = psiClass.project
        val scope = GlobalSearchScope.allScope(project)
        val stringType = PsiType.getTypeByName("java.lang.String", project, scope)
        val stringArrayType = stringType.createArrayType()
        for (field in fields) {
            val type = field.type
            val deepType = field.type.deepComponentType
            val isArray = type is PsiArrayType
            if (deepType == PsiType.BOOLEAN || deepType == PsiType.BYTE || deepType == PsiType.CHAR || deepType == PsiType.SHORT
                    || deepType == PsiType.INT || deepType == PsiType.LONG || deepType == PsiType.FLOAT || deepType == PsiType.DOUBLE) {
                val typeName = WordUtils.capitalize(deepType.getPresentableText())
                writeField(builder, field.name, typeName, isArray)
            } else if (deepType is PsiClassReferenceType) {
                if (type == stringType) {
                    writeField(builder, field.name, "UTF", false)
                } else if (type == stringArrayType) {
                    writeField(builder, field.name, "UTF", true)
                } else {
                    writeField(builder, field.name, "Object", false)
                }
            }
        }
    }

    private fun writeField(builder: StringBuilder, fieldName: String?, fieldType: String?, isArray: Boolean) {
        builder.append("""out.write$fieldType${if (isArray) "Array" else ""}($fieldName);""")
    }

    private fun generateRead(psiClass: PsiClass, fields: List<PsiField>) {
        val builder = StringBuilder("@Override\n")
        builder.append("public void readData(ObjectDataInput in) throws IOException {\n")
        readFields(psiClass, fields, builder)
        builder.append("}")
        setNewMethod(psiClass, builder.toString(), "readData")
    }

    private fun readFields(psiClass: PsiClass, fields: List<PsiField>, builder: StringBuilder) {
        val project = psiClass.project
        val scope = GlobalSearchScope.allScope(project)
        val stringType = PsiType.getTypeByName("java.lang.String", project, scope)
        val stringArrayType = stringType.createArrayType()
        for (field in fields) {
            val type = field.type
            val deepType = field.type.deepComponentType
            val isArray = type is PsiArrayType
            if (deepType == PsiType.BOOLEAN || deepType == PsiType.BYTE || deepType == PsiType.CHAR || deepType == PsiType.SHORT
                    || deepType == PsiType.INT || deepType == PsiType.LONG || deepType == PsiType.FLOAT || deepType == PsiType.DOUBLE) {
                val typeName = WordUtils.capitalize(deepType.getPresentableText())
                readField(builder, field.name, typeName, isArray)
            } else if (deepType is PsiClassReferenceType) {
                if (type == stringType) {
                    readField(builder, field.name, "UTF", false)
                } else if (type == stringArrayType) {
                    readField(builder, field.name, "UTF", true)
                } else {
                    readField(builder, field.name, "Object", false)
                }
            }
        }
    }

    private fun readField(builder: StringBuilder, fieldName: String?, fieldType: String, isArray: Boolean) {
        builder.append("""this.$fieldName = in.read$fieldType${if (isArray) "Array" else ""}();""")
    }
}
