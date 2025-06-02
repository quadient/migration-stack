package com.quadient.wfdxml.internal.layoutnodes.flow;

import com.quadient.wfdxml.api.layoutnodes.Element;
import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.Image;
import com.quadient.wfdxml.api.layoutnodes.TextStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.flow.Text;
import com.quadient.wfdxml.api.layoutnodes.tables.Table;
import com.quadient.wfdxml.internal.layoutnodes.TextStyleImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

import static com.quadient.wfdxml.utils.SplitBySeparator.splitByWholeSeparatorWorker;

public class TextImpl implements Text, XmlExportable {
    private final TabExportable tabExportable = new TabExportable();
    private final BrExportable brExportable = new BrExportable();
    private TextStyleImpl textStyle;
    private String existingTextStyleId = null;
    private List<XmlExportable> content = new ArrayList<>();

    private ParagraphImpl parent;

    public TextImpl() {
    }

    public TextImpl(ParagraphImpl parent) {
        this.parent = parent;
    }

    @Override
    public Text setTextStyle(TextStyle textStyle) {
        this.textStyle = (TextStyleImpl) textStyle;
        return this;
    }

    @Override
    public Text setExistingTextStyle(String id) {
        this.existingTextStyleId = id;
        return this;
    }

    @Override
    public Text appendText(String text) {
        if (text == null) {
            return this;
        }
        content.add(new StringExportable(text));

        replaceSpecialChar("\t", tabExportable);
        replaceSpecialChar("\n", brExportable);

        return this;
    }

    @Override
    public Text appendFlow(Flow flow) {
        return append(flow);
    }

    @Override
    public Text appendVariable(Variable variable) {
        return append(variable);
    }

    @Override
    public Text appendImage(Image image) {
        return append(image);
    }

    @Override
    public Text appendElement(Element element) {
        return append(element);
    }

    @Override
    public Text appendTable(Table table) {
        return append(table);
    }


    private Text append(Object obj) {
        content.add(new ObjectExportable(obj));
        return this;
    }

    @Override
    public ParagraphImpl back() {
        if (parent == null) {
            throw new IllegalStateException("Parent is not set");
        }
        return parent;
    }


    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("T")
                .addStringAttribute("xml:space", "preserve");

        if (textStyle != null) {
            exporter.addIfaceAttribute("Id", textStyle);
        } else if (existingTextStyleId != null) {
            exporter.addStringAttribute("Id", existingTextStyleId);
        }

        for (XmlExportable o : content) {
            o.export(exporter);
        }

        exporter.endElement();
    }

    private void replaceSpecialChar(String separator, XmlExportable classType) {
        List<XmlExportable> parts = new ArrayList<>();

        for (XmlExportable exp : content) {
            if (exp instanceof StringExportable) {
                List<String> textParts = splitByWholeSeparatorWorker(((StringExportable) exp).getString(), separator);
                for (int i = 0; i < textParts.size(); i++) {
                    parts.add(new StringExportable(textParts.get(i)));
                    if (i < textParts.size() - 1)
                        parts.add(classType);
                }
            } else {
                parts.add(exp);
            }
        }
        content = parts;
    }

    private class TabExportable implements XmlExportable {
        private TabExportable() {
        }

        @Override
        public void export(XmlExporter exporter) {
            exporter.addElement("Tab");
        }
    }

    private class BrExportable implements XmlExportable {
        private BrExportable() {
        }

        @Override
        public void export(XmlExporter exporter) {
            exporter.addElement("BR");
        }
    }

    private class StringExportable implements XmlExportable {
        private final String string;

        private StringExportable(String string) {
            this.string = string;
        }

        @Override
        public void export(XmlExporter exporter) {
            exporter.addPCData(string);
        }

        public String getString() {
            return string;
        }
    }

    private class ObjectExportable implements XmlExportable {
        private final Object object;

        private ObjectExportable(Object object) {
            this.object = object;
        }

        @Override
        public void export(XmlExporter exporter) {
            exporter.beginElement("O")
                    .addIfaceAttribute("Id", object)
                    .endElement();
        }
    }
}