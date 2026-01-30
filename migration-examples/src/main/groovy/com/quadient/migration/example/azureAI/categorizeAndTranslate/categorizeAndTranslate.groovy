package com.quadient.migration.example.azureAI.categorizeAndTranslate

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef

import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleDefinitionBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleDefinitionBuilder
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import groovy.json.JsonSlurper

import static com.quadient.migration.api.dto.migrationmodel.builder.Dsl.table


void documentsCategorize(List jsons, migration){
    println("Starting categorization and translation of documents...")

    //Create default style
    TextStyleBuilder textStyleBuilder = new TextStyleBuilder("normal")
    TextStyleDefinitionBuilder textStyle = new TextStyleDefinitionBuilder()
    textStyle.setSize(Size.ofPoints(11))
    textStyle.setFontFamily("Arial")
    migration.textStyleRepository.upsert(textStyleBuilder.definition(textStyle.build()).build())

    //Create default Paragraph style
    ParagraphStyleBuilder paragraphStyleBuilder = new ParagraphStyleBuilder("default")

    ParagraphStyleDefinitionBuilder paragraphStyleDefinitionBuilder = new ParagraphStyleDefinitionBuilder()
    paragraphStyleDefinitionBuilder.setLineSpacing(new LineSpacing.Additional(Size.ofMillimeters(-1.05)))

    paragraphStyleBuilder.setDefinition(paragraphStyleDefinitionBuilder.build())
    migration.paragraphStyleRepository.upsert(paragraphStyleBuilder.build())

    for(json in jsons){
        documentCategorize(json, migration)
    }
}

void documentCategorize(File jsonContent, migration){
    println("Starting: " + jsonContent.name)
    String templateName = jsonContent.name.replace(".json", "")
    def jsonSlurper = new JsonSlurper()

    def parsedData = jsonSlurper.parseText(jsonContent.text).get("analyzeResult")
    DocumentParser parser = new DocumentParser(parsedData, templateName, jsonContent.parent, migration)

    String content = "";
    DocumentObjectBuilder templateBuilder = new DocumentObjectBuilder(templateName, DocumentObjectType.Template)
            .name(templateName)
            .originLocations([jsonContent.absolutePath])
            .addCustomField("source", "Template")
            .addCustomField("type", "Template")

    String usedObjects = ""

    //Go through sections and get content
    if(parser.sections != null){
        usedObjects = parser.getContentFirst(parser.sections, migration)
    }
    templateBuilder.addCustomField("usedObjects", usedObjects)

    migration.documentObjectRepository.upsert(templateBuilder.build())
    def documentObjects = migration.documentObjectRepository.listAll()

    documentObjects.each { documentObject ->
        {
            ArrayList content1 = new ArrayList()
            if (documentObject.type.toString() != "Block" && documentObject.type.toString() != "Page") {
                content1 = new translateSectionsTemplatesGroups().translateSectionsTemplatesGroups(documentObject, migration)
                migration.documentObjectRepository.upsert(documentObject)

                if (documentObject.displayRuleRef != null || content1.size() > 0) {
                    documentObject.content = content1

                    migration.documentObjectRepository.upsert(documentObject)
                }
            }
        }

    }

    //print all results
    //println(content)

    //Print all content out of sections
    println("\n------OUT OF SECTIONS------")
    for(int j = 0; j < parser.paragraphs.size(); j++){
        if(!parser.paragraphsOutOfSections.contains(j+1)){
            println(parser.paragraphs.get(j).get("content"))
        }
    }
}

//Document parser logic
class DocumentParser {
    List indexes
    List paragraphsOutOfSections
    int index
    ArrayList sections
    ArrayList figures
    ArrayList paragraphs
    ArrayList tables
    ArrayList styles
    ArrayList pagesElement
    HashMap<Integer, String> pages = new HashMap()
    HashMap<String, List<Area>> flowAreas = new HashMap<>()
    String templateName = ""
    String source
    Integer lastParagraphNumber = 0

    static Integer counter = 0
    int buffer = 0 // 1.10 pt (0.388056 mm) around the polygon

    Migration migration

    DocumentParser(def parsedData, templateName, source, migration){
        indexes = new ArrayList<int>()
        paragraphsOutOfSections = new ArrayList<int>()
        sections = parsedData.get("sections")
        figures = parsedData.get("figures")
        paragraphs = parsedData.get("paragraphs")
        tables = parsedData.get("tables")
        styles = parsedData.get("styles")
        pagesElement = parsedData.get("pages")
        this.templateName = templateName
        this.source = source
        this.migration = migration
    }

    String getContentFirst(ArrayList parser, Migration migration) {
        for(int i = 0; i < parser.size(); i++){
            if(!indexes.contains(i)) {
                for (element in parser[i].get("elements")) {
                    getContent(element, migration)
                }
            }
        }
        for(int j = 0; j < paragraphs.size(); j++){
            if(!paragraphsOutOfSections.contains(j)){
                getContent("/paragraphs/" + (j).toString(), migration)
            }
        }


        int pageCounter = 0;

        String name = ""
        pages.each {
            pageCounter++
            String namePage = "P_" + templateName + "_" + it.key.toString()

            if(name.isEmpty())
                name = namePage
            else
                name = name + "," + namePage

            DocumentObjectBuilder sectionBuilder = new DocumentObjectBuilder(namePage, DocumentObjectType.Page)
            ArrayList content = new ArrayList()
            String usedObjects = ""
            for(String val in it.value){
                if(usedObjects.isEmpty()){
                    usedObjects = val
                } else {
                    usedObjects = usedObjects + "," + val
                }

                content.add(flowAreas.get(val))

            }

            sectionBuilder
                    .name(namePage)
                    .originLocations([""])
                    .addCustomField("usedBy", templateName)
                    .addCustomField("usedObjects", usedObjects)
                    .addCustomField("type", "Page")
                    .addCustomField("source", "Page").internal(false)

            sectionBuilder.content(content)
            if(pagesElement.get(it.key-1).width != null && pagesElement.get(it.key-1).height != null && pagesElement.get(it.key-1).width.toDouble() > pagesElement.get(it.key-1).height.toDouble()) {
                sectionBuilder.options(new PageOptions(Size.ofMillimeters(297), Size.ofMillimeters(210))) // Landscape
            } else {
                sectionBuilder.options(new PageOptions(Size.ofMillimeters(210), Size.ofMillimeters(297))) // Portrait
            }
            sectionBuilder.internal(false)
            migration.documentObjectRepository.upsert(sectionBuilder.build())
        }
        return name;
    }

    def buildStyledTextBuilders = { int paragraphOffset, String paragraphText, Size fontSize ->
        def paragraphLength = paragraphText.length()
        def paragraphEnd = paragraphOffset + paragraphLength
        def styleMap = new HashMap<Integer, Set<String>>()

        styles.each { styleBlock ->
            def weight = (styleBlock.fontWeight ?: "normal").toLowerCase()
            def italic = (styleBlock.fontStyle == "italic") ? "italic" : null

            styleBlock.spans.each { span ->
                def spanStart = span.offset
                def spanEnd = span.offset + span.length

                for (int i = spanStart; i < spanEnd; i++) {
                    if (i >= paragraphOffset && i < paragraphEnd) {
                        def idx = i - paragraphOffset
                        def styleSet = styleMap.getOrDefault(idx, new HashSet<String>())
                        if (weight == "bold") styleSet.add("bold")
                        if (italic == "italic") styleSet.add("italic")
                        styleMap.put(idx, styleSet)
                    }
                }
            }
        }

        def textBuilders = []
        def buffer = new StringBuilder()
        Set<String> currentStyle = null

        paragraphText.eachWithIndex { ch, idx ->
            def charStyles = styleMap.getOrDefault(idx, new HashSet<String>())

            if (currentStyle == null || charStyles != currentStyle) {
                // Flush buffer
                if (buffer.length() > 0) {
                    def styleRef = buildStyleRef(currentStyle, fontSize)
                    ensureTextStyleExists(styleRef, currentStyle, fontSize)

                    textBuilders << new ParagraphBuilder.TextBuilder()
                            .styleRef(styleRef)
                            .content(buffer.toString())
                    buffer.setLength(0)
                }

                currentStyle = charStyles
            }

            buffer.append(ch)
        }

        // Flush last buffer
        if (buffer.length() > 0) {
            def styleRef = buildStyleRef(currentStyle, fontSize)
            ensureTextStyleExists(styleRef, currentStyle, fontSize)

            textBuilders << new ParagraphBuilder.TextBuilder()
                    .styleRef(styleRef)
                    .content(buffer.toString())
        }

        return textBuilders
    }

    def ensureTextStyleExists = { String styleRef, Set<String> styleSet, Size fontSize ->
        if (styleRef != null && migration.textStyleRepository.find(styleRef) == null) {
            TextStyleBuilder textStyleBuilder = new TextStyleBuilder(styleRef)
            TextStyleDefinitionBuilder textStyle = new TextStyleDefinitionBuilder()
            textStyle.setSize(fontSize)
            textStyle.setFontFamily("Arial")

            if (styleSet?.contains("bold")) textStyle.setBold(true)
            if (styleSet?.contains("italic")) textStyle.setItalic(true)

            migration.textStyleRepository.upsert(
                    textStyleBuilder.definition(textStyle.build()).build()
            )
        }
    }

    // create styleRef based on styles
    def buildStyleRef = { Set<String> styleSet, Size fontSize ->
        def parts = ["normal"]
        if (styleSet?.contains("bold")) parts << "bold"
        if (styleSet?.contains("italic")) parts << "italic"
        if (fontSize != null) parts << fontSize.toPoints().toString()
        return parts.join("_")
    }

    def getFontSize = { bbox ->
        def yValues = [bbox[1], bbox[3], bbox[5], bbox[7]]
        def minY = yValues.min()
        def maxY = yValues.max()
        double boxHeight = maxY - minY

        double rawSize = boxHeight * 72

        double safeSize = Math.floor(rawSize * 0.85)

        return Math.max(safeSize, 4)
    }

    String getContent(String element, Migration migration) {
        String content = "";
        String name = ""
        def temp = element.split("/")

        if(temp[1] == "sections") {
            for (value in sections.get(temp[2].toInteger()).get("elements")) {
                indexes.add(temp[2].toInteger())
                getContent(value, migration)
            }
        }
        else if(temp[1] == "figures"){
            name = "F_" + templateName + "_" + temp[2]

            String usedObjects = ""
            String imageName = ""
            for (value in figures.get(temp[2].toInteger()).get("elements") as List) {
                Integer tempNumber = value.split("/")[2].toInteger()
                if(imageName.isEmpty()){
                    imageName = paragraphs.get(tempNumber).get("content")
                } else {
                    imageName = imageName + "_" + paragraphs.get(tempNumber).get("content")
                }
                paragraphsOutOfSections.add(tempNumber)
            }
            if(imageName.isEmpty()){
                imageName = name
            }

            ImageBuilder imageBuilder = new ImageBuilder(name)
                    .name(imageName)
                    .originLocations([""])
                    .addCustomField("usedObjects", usedObjects)
                    .imageType(ImageType.Png)
                    .sourcePath(migration.projectConfig.inputDataPath + "/" + imageName + ".png")

            Map<String, Double> positions = getPositionsFromBoundingRegion(figures.get(temp[2].toInteger()).get("boundingRegions"), false)
            if (positions.size() > 0) {
                positions.each { imageBuilder.addCustomField(it.key, it.value.toString()) }
                Size x = Size.ofMillimeters(positions["left-position"])
                Size y = Size.ofMillimeters(positions["top-position"])
                Size w = Size.ofMillimeters(positions["right-position"] - positions["left-position"] + buffer)
                Size h = Size.ofMillimeters(positions["bottom-position"] - positions["top-position"] + buffer)

                flowAreas.put(name, new Area([new ImageRef(name)],new Position(x, y, w, h),name))
                pages.computeIfAbsent(counter) { new ArrayList() }.add(name)
            }

            imageBuilder.addCustomField("usedBy", "P_" + counter.toString())

            migration.imageRepository.upsert(imageBuilder.build())
        }
        else if(temp[1] == "paragraphs"){
                def paraSpan = paragraphs.get(temp[2].toInteger()).get("spans")[0]
                def paraStart = paraSpan.offset
                def paraEnd = paraStart + paraSpan.length

                def lineMatch
                pagesElement.find{ pages ->
                    lineMatch = pages.lines.find{ line ->
                        def span = line.spans[0]
                        def lineStart = span.offset
                        def lineEnd = lineStart + span.length
                        return (lineStart >= paraStart && lineEnd <= paraEnd)
                    }
                }

                def fontSize
                String styleRef = "normal"
                if (lineMatch) {
                    fontSize = Size.ofPoints(getFontSize(lineMatch.polygon))
                    String tempRefName = styleRef + "_" + fontSize.toPoints().toString()
                    if(migration.textStyleRepository.find(tempRefName) == null){
                        TextStyleBuilder textStyleBuilder = new TextStyleBuilder(tempRefName)
                        TextStyleDefinitionBuilder textStyle = new TextStyleDefinitionBuilder()
                        textStyle.setSize(fontSize)
                        textStyle.setFontFamily("Arial")
                        migration.textStyleRepository.upsert(textStyleBuilder.definition(textStyle.build()).build())
                        styleRef = tempRefName
                    }else {
                        styleRef = tempRefName
                    }
                }
                //-----------------------------------
                name = "Text_" + templateName + "_" + temp[2]

                def paragraphText = paragraphs.get(temp[2].toInteger()).get("content").toString()
                def styledTexts = buildStyledTextBuilders(paraSpan.offset, paragraphText, fontSize)

                Map<String, Double> positions = getPositionsFromBoundingRegion(paragraphs.get(temp[2].toInteger()).get("boundingRegions"), false)

                def tempContent = [new ParagraphBuilder().styleRef("default").content(styledTexts).build()]

                DocumentObjectBuilder paragraphBuilder
                if(paragraphText == ":barcode:"){
                    String nameBarcode = "barcode" + getBarcodeType(counter)
                    paragraphBuilder = new DocumentObjectBuilder(nameBarcode, DocumentObjectType.Block)
                            .name(nameBarcode)
                            .originLocations([""])
                            .addCustomField("originalContent", paragraphText)
                            .addCustomField("type","Text")
                            .addCustomField("source","Block")
                            .content(tempContent)
                            .internal(false)
                    tempContent = [new DocumentObjectRef(nameBarcode)]
                    migration.documentObjectRepository.upsert(paragraphBuilder.build())
                }

                paragraphBuilder = new DocumentObjectBuilder(name, DocumentObjectType.Block)
                        .name(name)
                        .originLocations([""])
                        .addCustomField("originalContent", paragraphText)
                        .addCustomField("type","Text")
                        .addCustomField("source","Block")
                        .content(tempContent)
                        .internal(true)


                if (positions.size() > 0) {
                    positions.each { paragraphBuilder.addCustomField(it.key, it.value.toString()) }
                    Size x = Size.ofMillimeters(positions["left-position"])
                    Size y = Size.ofMillimeters(positions["top-position"])
                    Size w = Size.ofMillimeters(positions["right-position"] - positions["left-position"] + buffer)
                    Size h = Size.ofMillimeters(positions["bottom-position"] - positions["top-position"] + buffer)

                    paragraphBuilder.addCustomField("usedBy", "P_" + counter.toString())
                    flowAreas.put(name, new Area([new DocumentObjectRef(name)], new Position(x, y, w, h), name))
                    pages.computeIfAbsent(counter) { new ArrayList() }.add(name)
                }

                paragraphsOutOfSections.add(temp[2].toInteger())

                migration.documentObjectRepository.upsert(paragraphBuilder.build())
        }
        else if(temp[1] == "tables" && tables != null){
            Map tempCellPositions = [:]

            name = "Table_" + templateName + "_" + (temp[2].toInteger()+1000).toString()
            DocumentObjectBuilder paragraphBuilder = new DocumentObjectBuilder(name, DocumentObjectType.Block)
                    .name(name)
                    .originLocations([""])
                    .addCustomField("type","Text")
                    .addCustomField("source","Block")
                    .internal(true)

            List parsingResult = parseTable(tables.get(temp[2].toInteger()), tempCellPositions)
            //-------------Uncomment if you want to have table in the content-------------------------
            /*paragraphBuilder.content([parsingResult[0]])
            tempCellPositions = parsingResult[1]

            //Map<String, Double> positions = getPositionsFromBoundingRegion(paragraphs.get(temp[2].toInteger()).get("boundingRegions"), false)
            if (tempCellPositions.size() > 0) {
                tempCellPositions.each { paragraphBuilder.addCustomField(it.key.toString(), it.value.toString()) }
                Size x = Size.ofMillimeters(tempCellPositions["left-position"])
                Size y = Size.ofMillimeters(tempCellPositions["top-position"])
                Size w = Size.ofMillimeters(tempCellPositions["right-position"] - tempCellPositions["left-position"] + buffer)
                Size h = Size.ofMillimeters(tempCellPositions["bottom-position"] - tempCellPositions["top-position"] + buffer)
                tempCellPositions.clear()

                paragraphBuilder.addCustomField("usedBy", "P_" + counter.toString())
                //flowAreas.put(name, new FlowArea(new Position(x, y, w, h), [new DocumentObjectRef(name)]))
                flowAreas.put(name, new Area([new DocumentObjectRef(name)], new Position(x, y, w, h), name))
                pages.computeIfAbsent(counter) { new ArrayList() }.add(name)
            }

            migration.documentObjectRepository.upsert(paragraphBuilder.build())*/
        }
        else {
            println("|OUT OF OUR KNOWLEDGE|")
        }

        return name;
    }

    String getBarcodeType(Integer pageindex){
        def page = pagesElement.get(pageindex-1)
        String barcodeType = ""
        page.barcodes.each { barcode ->
            barcodeType = barcode.kind
        }
        return barcodeType
    }


     List parseTable(def tableInput, Map tempCellPositions) {

        Table tableModel = table { table ->

            def firstRow = true

            def rows = [:].withDefault { [] }

            def maxColumn = 0

            for (cell in tableInput.get("cells")) {
                def rowIndex = cell.get("rowIndex")
                def columnIndex = cell.get("columnIndex").toInteger()
                def columnSpan = (cell.get("columnSpan") ?: 1).toInteger()

                rows[rowIndex] << cell

                def columnEnd = columnIndex + columnSpan
                if (columnEnd > maxColumn) {
                    maxColumn = columnEnd
                }

                for (value in cell.get("elements")) {
                    def temp = value.split("/")

                    if(temp[1] == "paragraphs"){
                        getContent(value, migration)
                        paragraphsOutOfSections.add(temp[2].toInteger())
                    }
                }
            }

            rows.keySet().sort().each { rowIndex ->

                table.row { row ->

                    if (firstRow) {
                        firstRow = false
                    }

                    def cellMap = [:]
                    rows[rowIndex].each { currentCell ->
                        def colIndex = currentCell.get("columnIndex").toInteger()
                        cellMap[colIndex] = currentCell
                    }

                    int currentColumn = 0
                    while (currentColumn < maxColumn) {

                        def currentCell = cellMap[currentColumn]

                        if (currentCell) {
                            def columnSpan = (currentCell.get("columnSpan") ?: 1).toInteger()

                            row.cell { cell ->
                                cell.content(new Paragraph(currentCell.get("content")))
                            }

                            if (columnSpan > 1) {
                                for (int i = 1; i < columnSpan; i++) {
                                    row.cell { emptyCell ->
                                        emptyCell.content(new Paragraph(""))
                                        emptyCell.mergeLeft = true
                                    }
                                }
                            }

                            currentColumn += columnSpan

                            tempCellPositions = getPositionsFromBoundingRegion(currentCell.get("boundingRegions"), true, tempCellPositions)
                        } else {
                            row.cell { emptyCell ->
                                emptyCell.content(new Paragraph(""))
                            }
                            currentColumn += 1
                        }
                    }

                } // end table.row

            } // end each rowIndex

        } // end table

        return [tableModel, tempCellPositions]
    }

    static Map<String, Double> getPositionsFromBoundingRegion(def boundingRegion, boolean isTable) {
        getPositionsFromBoundingRegion(boundingRegion, isTable, [:])
    }

    static Map<String, Double> getPositionsFromBoundingRegion(def boundingRegion, boolean isTable, HashMap tempCellPositions) {
        Map<String, Double> positions = [:]
        counter = boundingRegion[0].get("pageNumber")
        def polygon = boundingRegion[0].get("polygon")

        if (polygon && polygon.size() >= 8) {

            def xCoords = (0..<polygon.size()).findAll { it % 2 == 0 }.collect { (polygon[it].toDouble() * 25.4) }
            def yCoords = (0..<polygon.size()).findAll { it % 2 == 1 }.collect { (polygon[it].toDouble() * 25.4) }

            if(isTable && tempCellPositions.size() > 0) {
                tempCellPositions.put("right-position", xCoords.max())
                tempCellPositions.put("bottom-position", yCoords.max())
                return tempCellPositions
            } else {
                positions.put("left-position", xCoords.min())
                positions.put("right-position", xCoords.max())
                positions.put("top-position", yCoords.min())
                positions.put("bottom-position", yCoords.max())
            }
        }

        return positions
    }
}
