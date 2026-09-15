package com.quadient.migration.example.docx

import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType

/** Builds Word field markup shared by the DOCX parser tests. */
class DocxFieldFixtures {

    static void appendMergeField(paragraph, String mergeInstruction, String cachedResult = null) {
        fieldCharacter(paragraph.createRun(), STFldCharType.BEGIN)
        instruction(paragraph.createRun(), mergeInstruction)
        if (cachedResult != null) {
            fieldCharacter(paragraph.createRun(), STFldCharType.SEPARATE)
            paragraph.createRun().setText(cachedResult)
        }
        fieldCharacter(paragraph.createRun(), STFldCharType.END)
    }

    static void appendEqualityIfField(paragraph, String variable, String operand, String result) {
        fieldCharacter(paragraph.createRun(), STFldCharType.BEGIN)
        instruction(paragraph.createRun(), " IF ")
        fieldCharacter(paragraph.createRun(), STFldCharType.BEGIN)
        instruction(paragraph.createRun(), " MERGEFIELD ${variable} ")
        fieldCharacter(paragraph.createRun(), STFldCharType.END)
        instruction(paragraph.createRun(), " = ${operand} \"${result}\" \"\" ")
        fieldCharacter(paragraph.createRun(), STFldCharType.END)
    }

    static void fieldCharacter(run, STFldCharType.Enum type) {
        run.CTR.addNewFldChar().setFldCharType(type)
    }

    static void instruction(run, String value) {
        run.CTR.addNewInstrText().setStringValue(value)
    }
}
