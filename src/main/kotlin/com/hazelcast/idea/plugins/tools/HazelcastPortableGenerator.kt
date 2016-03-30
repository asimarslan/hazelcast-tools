package com.hazelcast.idea.plugins.tools

import java.util.ArrayList

import org.apache.commons.lang.WordUtils

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiType
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope

class HazelcastPortableGenerator : UtilityGeneration("Generate Hazelcast Portable write and read", "Select fields for portable") {

    override fun generate(psiClass: PsiClass, fields: List<PsiField>) {
        object : WriteCommandAction.Simple<Unit>(psiClass.project, psiClass.containingFile) {
            override fun run() {
                generatePortableWrite(psiClass, fields)
                generatePortableRead(psiClass, fields)
            }
        }.execute()
    }


    private fun generatePortableWrite(psiClass: PsiClass, fields: List<PsiField>) {
        val builder = StringBuilder("@Override\n")
        builder.append("public void writePortable(PortableWriter out) throws IOException {\n")

        val remaining = writeNativePortableFields(psiClass, fields, builder)
        writeObjects(psiClass, remaining, builder)

        builder.append("}")
        setNewMethod(psiClass, builder.toString(), "writePortable")
    }

    private fun writeNativePortableFields(psiClass: PsiClass, fields: List<PsiField>, builder: StringBuilder): List<PsiField> {
        val remaining = ArrayList<PsiField>()
        val project = psiClass.project
        val scope = GlobalSearchScope.allScope(project)
        val portableType = PsiType.getTypeByName("com.hazelcast.nio.serialization.Portable", project, scope)
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
                if (portableType.isAssignableFrom(deepType)) {
                    writeField(builder, field.name, "Portable", isArray)
                } else if (type == stringType) {
                    writeField(builder, field.name, "UTF", false)
                } else if (type == stringArrayType) {
                    writeField(builder, field.name, "UTF", true)
                } else {
                    remaining.add(field)
                }
            } else {
                remaining.add(field)
            }
        }
        return remaining
    }

    private fun writeObjects(psiClass: PsiClass, fields: List<PsiField>, builder: StringBuilder) {
        var rawWritten = false
        val project = psiClass.project
        val scope = GlobalSearchScope.allScope(project)
        val dataserializableType = PsiType.getTypeByName("com.hazelcast.nio.serialization.DataSerializable", project, scope)
        for (field in fields) {
            val type = field.type
            val deepType = field.type.deepComponentType
            if (type is PsiClassReferenceType) {
                val tt = deepType as PsiClassReferenceType
                if (dataserializableType.isAssignableFrom(tt)) {
                    if (!rawWritten) {
                        rawWritten = true
                        builder.append("ObjectDataOutput rawDataOutput = out.getRawDataOutput();")
                    }
                    val fieldNameT = WordUtils.capitalize(field.name)
                    builder.append(
"""boolean isNotNull$fieldNameT = ${field.name} != null;
if (isNotNull$fieldNameT) {
    rawDataOutput.writeBoolean(isNotNull$fieldNameT);
    ${field.name}.writeData(rawDataOutput);
} else {
    rawDataOutput.writeBoolean(isNotNull$fieldNameT);
}""")
                } else {
                    if (!rawWritten) {
                        rawWritten = true
                        builder.append("ObjectDataOutput rawDataOutput = out.getRawDataOutput();")
                    }
                    builder.append("rawDataOutput.writeObject(${field.name});")
                }
            }
        }
    }

    private fun writeField(builder: StringBuilder, fieldName: String?, fieldType: String?, isArray: Boolean) {
        builder.append("""out.write$fieldType${if (isArray) "Array" else ""}("$fieldName", $fieldName);""")
    }

    private fun generatePortableRead(psiClass: PsiClass, fields: List<PsiField>) {
        val builder = StringBuilder("@Override\n")
        builder.append("public void readPortable(PortableReader in) throws IOException {\n")

        val remaining = readNativePortableFields(psiClass, fields, builder)
        readObjects(psiClass, remaining, builder)

        builder.append("}")
        setNewMethod(psiClass, builder.toString(), "readPortable")
    }

    private fun readNativePortableFields(psiClass: PsiClass, fields: List<PsiField>, builder: StringBuilder): List<PsiField> {
        val remaining = ArrayList<PsiField>()
        val project = psiClass.project
        val scope = GlobalSearchScope.allScope(project)
        val portableType = PsiType.getTypeByName("com.hazelcast.nio.serialization.Portable", project, scope)
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
                if (portableType.isAssignableFrom(deepType)) {
                    if (isArray) {
                        builder.append(
                        """Portable[] ${field.name} = in.readPortableArray("${field.name}");
                        this.${field.name} = new ${deepType.className}[${field.name}.length];
                        System.arraycopy(${field.name}, 0, this.${field.name}, 0, ${field.name}.length);
                        """)
                    } else {
                        readField(builder, field.name, "Portable", isArray)
                    }
                } else if (type == stringType) {
                    readField(builder, field.name, "UTF", false)
                } else if (type == stringArrayType) {
                    readField(builder, field.name, "UTF", true)
                } else {
                    remaining.add(field)
                }
            } else {
                remaining.add(field)
            }
        }
        return remaining
    }

    private fun readObjects(psiClass: PsiClass, fields: List<PsiField>, builder: StringBuilder) {
        var rawWritten = false
        val project = psiClass.project
        val scope = GlobalSearchScope.allScope(project)
        val dataserializableType = PsiType.getTypeByName("com.hazelcast.nio.serialization.DataSerializable", project, scope)
        for (field in fields) {
            val type = field.type
            val deepType = field.type.deepComponentType
            if (type is PsiClassReferenceType) {
                val tt = deepType as PsiClassReferenceType
                if (dataserializableType.isAssignableFrom(tt)) {
                    if (!rawWritten) {
                        rawWritten = true
                        builder.append("ObjectDataInput rawDataInput = in.getRawDataInput();\n")
                    }
                    val fieldNameT = WordUtils.capitalize(field.name)
                    builder.append(
"""boolean isNotNull$fieldNameT = rawDataInput.readBoolean();
if (isNotNull$fieldNameT) {
    ${tt.className} ${field.name} = new ${tt.className}();
    ${field.name}.readData(rawDataInput);
    this.${field.name} = ${field.name};
}""")
                } else {
                    if (!rawWritten) {
                        rawWritten = true
                        builder.append("ObjectDataInput rawDataInput = in.getRawDataInput();")
                    }
                    builder.append("this.${field.name} = rawDataInput.readObject();")
                }
            }
        }
    }

    private fun readField(builder: StringBuilder, fieldName: String?, fieldType: String?, isArray: Boolean) {
        builder.append("""this.$fieldName = in.read$fieldType${if (isArray) "Array" else ""}("$fieldName");""")
    }
}
