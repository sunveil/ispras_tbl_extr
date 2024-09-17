package writers;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.*;
import model.table.Cell;
import model.table.Row;
import model.table.Table;
import org.apache.commons.io.FilenameUtils;
import utils.Config;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

public class JaksonWriter {

    private final Document document;
    ObjectMapper jakson;
    ObjectNode root;
    int startPage = 0;
    int endPage = 0;
    boolean partialExtraction;
    ObjectWriter writer;;

    public JaksonWriter(Document document){
        this.document = document;
        this.partialExtraction = false;
        this.jakson = new ObjectMapper();
        this.root = this.jakson.createObjectNode();
        this.root.put("document", this.document.getSourceFile().getName());
        this.writer = this.jakson.writer(new DefaultPrettyPrinter());
    }

    public JaksonWriter(Document document, int startPage, int endPage){
        this.document = document;
        this.startPage = startPage;
        this.endPage = endPage;
        this.partialExtraction = true;
        this.jakson = new ObjectMapper();
        this.root = this.jakson.createObjectNode();
        this.root.put("document", this.document.getSourceFile().getName());
        this.writer = this.jakson.writer(new DefaultPrettyPrinter());
    }

    public void write() throws IOException {
        ArrayNode jsonPages = this.jakson.createArrayNode();
        if (partialExtraction) {
            for (int i = startPage; i <= endPage; i++) {
                Page page = document.getPage(i);
                jsonPages.add(writeJaksonPage(page));
            }
        } else {
            for (Iterator<Page> it = this.document.getPagesItrerator(); it.hasNext(); ) {
                Page page = it.next();
                jsonPages.add(writeJaksonPage(page));
            }
        }
        this.root.put("pages", jsonPages);
        File out = new File(Config.tmpDir + "data.json");
        this.writer.writeValue(out, this.root);
    }

    private ObjectNode writeJaksonPage(Page page) {
        //page.sortLines();
        ObjectNode jsonPage = this.jakson.createObjectNode();
        ArrayNode jsonBlocks = this.jakson.createArrayNode();
        ArrayNode jsonTables = this.jakson.createArrayNode();
        ArrayNode jsonImages = this.jakson.createArrayNode();
        jsonPage.put("number", page.getIndex());
        jsonPage.put("width", page.getWidth());
        jsonPage.put("height", page.getHeight());
        TextChunk prev_line = null;
        if (!page.getTextLines().isEmpty()) {
            prev_line = page.getTextLines().get(0);
        }
        List<TextChunk> outsideTextLine = page.getOutsideTextLines();
        for (TextChunk block: outsideTextLine) {
            ObjectNode jsonBlock =this.jakson.createObjectNode();
            ArrayNode jsonAnnotations = this.jakson.createArrayNode();
            jsonBlock.put("order", 10000 * (page.getIndex()+1) + block.getId());
            jsonBlock.put("x_top_left", (int)block.getLeft());
            jsonBlock.put("y_top_left", (int)block.getTop());
            jsonBlock.put("width", (int)block.getWidth());
            int height = (int)block.getHeight() < 0 ? 0: (int)block.getHeight();
            jsonBlock.put("height", height);
            jsonBlock.put("text", block.getText());
            jsonBlock.put("start", 0);
            jsonBlock.put("end", block.getText().length() - 1);
            if (!block.getMetadata().equals("")) {
                jsonBlock.put("metadata", block.getMetadata());
            } else {
                jsonBlock.put("metadata", "unknown");
            }
            jsonBlock.put("indent", (int) block.getLeft());
            int spacing = (int) (block.getTop() - prev_line.getBottom());
            if (spacing < 0) spacing = 0;
            jsonBlock.put("spacing", spacing);
            prev_line = block;
            int start = 0;
            for (TextChunk.TextLine chunk: block.getWords()){
                ObjectNode annotation = this.jakson.createObjectNode();
                if (!chunk.getMetadata().equals("")) {
                    annotation.put("metadata", chunk.getMetadata());
                } else {
                    annotation.put("metadata", "unknown");
                }
                annotation.put("url", chunk.getUrl());
                annotation.put("text", chunk.getText());
                annotation.put("is_bold", chunk.getFont().isBold());
                annotation.put("is_italic", chunk.getFont().isItalic());
                annotation.put("is_normal", chunk.getFont().isNormal());
                annotation.put("font_name", chunk.getFont().getName());
                annotation.put("font_size", (int)chunk.getFont().getFontSize());
                annotation.put("x_top_left", (int)chunk.getBbox().getLeft());
                annotation.put("y_top_left", (int)chunk.getBbox().getTop());
                annotation.put("width", (int)chunk.getBbox().getWidth());
                annotation.put("height", (int)chunk.getBbox().getHeight());
                annotation.put("start", start);
                int len = chunk.getText().length();
                annotation.put("end", start + len);
                start = start + len + 1;
                jsonAnnotations.add(annotation);
            }
            jsonBlock.put("annotations", jsonAnnotations);
            jsonBlocks.add(jsonBlock);
        }
        jsonPage.put("blocks",jsonBlocks);
        for (Table table: page.getTables()) {
            ObjectNode jsonTable = this.jakson.createObjectNode();
            jsonTable.put("x_top_left", (int)table.getLeft());
            jsonTable.put("y_top_left", (int)table.getTop());
            jsonTable.put("width", (int)table.getWidth());
            jsonTable.put("height", (int)table.getHeight());
            jsonTable.put("order", 10000 * (page.getIndex()+1) + table.getOrder());
            ArrayNode cellProperties = this.jakson.createArrayNode();
            ArrayNode jsonRows = this.jakson.createArrayNode();
            for (int i = 0; i < table.getNumOfRows(); i++) {
                ArrayNode jsonRow = this.jakson.createArrayNode();
                ArrayNode jsonPropertiesRow = this.jakson.createArrayNode();
                Row row = table.getRow(i);
                for (Cell cell: row.getCells()){
                    ObjectNode cellText = this.jakson.createObjectNode();
                    cellText.put("text", cell.getText());
                    ArrayNode cellBlocks = this.jakson.createArrayNode();
                    int start = 0;
                    for(TextChunk tb: page.getTextLines()) {
                        for (TextChunk.TextLine tl: tb.getWords()){
                            if (cell.intersects(tl.getBbox())){
                                ObjectNode cellBlock = this.jakson.createObjectNode();
                                cellBlock.put("x_top_left", (int)tl.getBbox().getLeft());
                                cellBlock.put("y_top_left", (int)tl.getBbox().getTop());
                                cellBlock.put("width", (int)tl.getBbox().getWidth());
                                cellBlock.put("height", (int)tl.getBbox().getHeight());
                                cellBlock.put("start", start);
                                int len = tl.getText().length();
                                cellBlock.put("end", start + len);
                                start = start + len + 1;
                                cellBlocks.add(cellBlock);

                            }
                        }
                    }

                    cellText.put("cell_blocks", cellBlocks);
                    jsonRow.add(cellText);
                    ObjectNode jsonProp = this.jakson.createObjectNode();
                    int rowSpan = cell.getRb() - cell.getRt() + 1;
                    jsonProp.put("row_span", rowSpan);
                    int colSpan = cell.getCr() - cell.getCl() + 1;
                    jsonProp.put("col_span", colSpan);
                    jsonProp.put("invisible", cell.getInvisiable());
                    jsonPropertiesRow.add(jsonProp);
                }
                if (!row.getCells().isEmpty()) {
                    jsonRows.add(jsonRow);
                    cellProperties.add(jsonPropertiesRow);
                }
            }
            jsonTable.put("rows", jsonRows);
            jsonTable.put("cell_properties", cellProperties);
            jsonTables.add(jsonTable);

        }

        jsonPage.put("tables",jsonTables);
        for (PDFImage image: page.getImages()) {
            ObjectNode jsonImage =  this.jakson.createObjectNode();
            jsonImage.put("original_name", image.getFileName());
            jsonImage.put("tmp_file_path", image.getPathOut());
            jsonImage.put("uuid", image.getUuid());
            jsonImage.put("x_top_left", image.getXPosition());
            jsonImage.put("y_top_left", image.getYPosition());
            jsonImage.put("width", image.getWidth());
            jsonImage.put("height", image.getHeight());
            jsonImage.put("page_num", image.getPageNumber());
            jsonImages.add(jsonImage);
        }
        jsonPage.put("images",jsonImages);

        return jsonPage;
    }


}
