package writers;

import model.*;
import model.table.Cell;
import model.table.Row;
import model.table.Table;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

public class JsonDocumentWriter {

    private final Document document;
    JSONObject json;
    int startPage = 0;
    int endPage = 0;
    boolean partialExtraction;

    public JsonDocumentWriter(Document document){
        this.document = document;
        this.json = new JSONObject();
        this.json.put("document", this.document.getSourceFile().getName());
        this.partialExtraction = false;
    }

    public JsonDocumentWriter(Document document, int startPage, int endPage){
        this.document = document;
        this.json = new JSONObject();
        this.json.put("document", this.document.getSourceFile().getName());
        this.startPage = startPage;
        this.endPage = endPage;
        this.partialExtraction = true;
    }

    public String write() {
        JSONArray jsonPages = new JSONArray();
        if (partialExtraction) {
            for (int i = startPage; i <= endPage; i++) {
                Page page = document.getPage(i);
                jsonPages.put(writePage(page));
            }
        } else {
            for (Iterator<Page> it = this.document.getPagesItrerator(); it.hasNext(); ) {
                Page page = it.next();
                jsonPages.put(writePage(page));
            }
        }
        json.put("pages", jsonPages);
        return json.toString();
    }

    private JSONObject writePage(Page page) {
        //page.sortLines();
        JSONObject jsonPage = new JSONObject();
        JSONArray jsonBlocks = new JSONArray();
        JSONArray jsonTables = new JSONArray();
        JSONArray jsonImages = new JSONArray();
        jsonPage.put("number", page.getIndex());
        jsonPage.put("width", page.getWidth());
        jsonPage.put("height", page.getHeight());
        TextChunk prev_line = null;
        if (!page.getTextLines().isEmpty()) {
            prev_line = page.getTextLines().get(0);
        }
        List<TextChunk> outsideTextLine = page.getOutsideTextLines();
        for (TextChunk block: outsideTextLine) {
            JSONObject jsonBlock = new JSONObject();
            JSONArray jsonAnnotations = new JSONArray();
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
                JSONObject annotation = new JSONObject();
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
                jsonAnnotations.put(annotation);
            }
            jsonBlock.put("annotations", jsonAnnotations);
            jsonBlocks.put(jsonBlock);
        }
        jsonPage.put("blocks",jsonBlocks);
        for (Table table: page.getTables()) {
            JSONObject jsonTable = new JSONObject();
            jsonTable.put("x_top_left", (int)table.getLeft());
            jsonTable.put("y_top_left", (int)table.getTop());
            jsonTable.put("width", (int)table.getWidth());
            jsonTable.put("height", (int)table.getHeight());
            jsonTable.put("order", 10000 * (page.getIndex()+1) + table.getOrder());
            JSONArray cellProperties = new JSONArray();
            JSONArray jsonRows = new JSONArray();
            for (int i = 0; i < table.getNumOfRows(); i++) {
                JSONArray jsonRow = new JSONArray();
                JSONArray jsonPropertiesRow = new JSONArray();
                Row row = table.getRow(i);
                for (Cell cell: row.getCells()){
                    JSONObject cellText = new JSONObject();
                    cellText.put("text", cell.getText());
                    JSONArray cellBlocks = new JSONArray();
                    int start = 0;
                    for(TextChunk tb: page.getTextLines()) {
                        for (TextChunk.TextLine tl: tb.getWords()){
                            if (cell.intersects(tl.getBbox())){
                                JSONObject cellBlock = new JSONObject();
                                cellBlock.put("x_top_left", (int)tl.getBbox().getLeft());
                                cellBlock.put("y_top_left", (int)tl.getBbox().getTop());
                                cellBlock.put("width", (int)tl.getBbox().getWidth());
                                cellBlock.put("height", (int)tl.getBbox().getHeight());
                                cellBlock.put("start", start);
                                int len = tl.getText().length();
                                cellBlock.put("end", start + len);
                                start = start + len + 1;
                                cellBlocks.put(cellBlock);

                            }
                        }
                    }

                    cellText.put("cell_blocks", cellBlocks);
                    jsonRow.put(cellText);
                    JSONObject jsonProp = new JSONObject();
                    int rowSpan = cell.getRb() - cell.getRt() + 1;
                    jsonProp.put("row_span", rowSpan);
                    int colSpan = cell.getCr() - cell.getCl() + 1;
                    jsonProp.put("col_span", colSpan);
                    jsonProp.put("invisible", cell.getInvisiable());
                    jsonPropertiesRow.put(jsonProp);
                }
                if (!row.getCells().isEmpty()) {
                    jsonRows.put(jsonRow);
                    cellProperties.put(jsonPropertiesRow);
                }
            }
            jsonTable.put("rows", jsonRows);
            jsonTable.put("cell_properties", cellProperties);
            jsonTables.put(jsonTable);

        }

        jsonPage.put("tables",jsonTables);
        for (PDFImage image: page.getImages()) {
            JSONObject jsonImage = new JSONObject();
            jsonImage.put("original_name", image.getFileName());
            jsonImage.put("tmp_file_path", image.getPathOut());
            jsonImage.put("uuid", image.getUuid());
            jsonImage.put("x_top_left", image.getXPosition());
            jsonImage.put("y_top_left", image.getYPosition());
            jsonImage.put("width", image.getWidth());
            jsonImage.put("height", image.getHeight());
            jsonImage.put("page_num", image.getPageNumber());
            jsonImages.put(jsonImage);
        }
        jsonPage.put("images",jsonImages);

        return jsonPage;
    }

}
