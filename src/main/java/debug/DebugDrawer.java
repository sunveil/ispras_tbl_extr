package debug;


import extractors.bordered.Range;
import model.*;
import model.table.Cell;
import model.table.Table;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class DebugDrawer {
    private static final String SUFFIX_START_CHAR = "_";

    private Document document;
    private Path debugDirectoryPath;

    private PDDocument recomposedDocument;

    private String chunkDirectoryName;
    private String charDirectoryName;
    private String wordDirectoryName;
    private String blockDirectoryName;
    private String rulingDirectoryName;
    private String borderedTableDirectoryName;
    private String recomposedDocumentDirectoryName;
    private String textLinesDirectoryName;
    private String textLinesFileNameSuffix;
    private String chunkFileNameSuffix;
    private String charFileNameSuffix;
    private String wordFileNameSuffix;
    private String blockFileNameSuffix;
    private String rulingFileNameSuffix;
    private String borderedTableFileNameSuffix;
    private String recomposedDocumentNameSuffix;

    private DrawStyle chunkDrawStyle;
    private DrawStyle charDrawStyle;
    private DrawStyle wordDrawStyle;
    private DrawStyle blockDrawStyle;
    private DrawStyle rulingDrawStyle;

    // Colors
    private static final Color BLUE = new Color(0, 102, 153);
    private static final Color GREEN = new Color(0, 153, 51);
    private static final Color RED = new Color(153, 0, 0);

    private DebugDrawer(Document document, Path debugDirectoryPath) {
        this.document = document;
        this.debugDirectoryPath = debugDirectoryPath;
    }

    public void drawBeforeRecomposing() throws IOException, ParserConfigurationException, TransformerException {
        drawChunks();
        //drawChars();
        drawWords();
        drawBlocks();
        drawRulings();
        drawBorderedTables();
        drawProjections();
        drawTextLines();
    }

    private PDDocument getPDDocument() throws IOException {
        File file = document.getSourceFile();
        PDDocument pdDocument = Loader.loadPDF(file);

        if (pdDocument.isEncrypted()) {
            pdDocument.setAllSecurityToBeRemoved(true);
        }

        return pdDocument;
    }

    private void drawChunks() throws IOException {
        PDDocument pdDocument = getPDDocument();
        PageDrawer.Builder builder = new PageDrawer.Builder(pdDocument, chunkDrawStyle);

        for (Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext(); ) {
            Page page = pages.next();
            int pageIndex = page.getIndex();

            PageDrawer drawer = builder.createPageDrawer(pageIndex);
            for (Iterator<TextChunk> chunks = page.getChunks(); chunks.hasNext(); ) {
                drawChunk(drawer, chunks.next());
            }
            drawer.close();
        }

        String outFilePath = getOutputFilePath(chunkDirectoryName, chunkFileNameSuffix);
        pdDocument.save(outFilePath);
        pdDocument.close();
    }

    private void drawChunk(PageDrawer drawer, TextChunk chunk) throws IOException {
        drawer.drawRectangle(chunk.getLeft(), chunk.getTop(), chunk.getRight(), chunk.getBottom());
        String s = String.valueOf(chunk.getStartOrder());
        drawer.drawString(s, chunk.getLeft(), chunk.getTop());
    }

/*    private void drawChars() throws IOException {
        PDDocument pdDocument = getPDDocument();
        PageDrawer.Builder builder = new PageDrawer.Builder(pdDocument, charDrawStyle);

        for (Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext(); ) {
            Page page = pages.next();
            int pageIndex = page.getIndex();

            PageDrawer drawer = builder.createPageDrawer(pageIndex);
            for (Iterator<TextChunk> chars = page.getChars(); chars.hasNext(); ) {
                drawChar(drawer, chars.next());
            }
            drawer.close();
        }

        String outFilePath = getOutputFilePath(charDirectoryName, charFileNameSuffix);
        pdDocument.save(outFilePath);
        pdDocument.close();
    }*/

    private void drawChar(PageDrawer drawer, TextChunk character) throws IOException {
        drawer.drawRectangle(character.getLeft(), character.getTop(), character.getRight(), character.getBottom());
    }

    private void drawWords() throws IOException, ParserConfigurationException, TransformerException {
        PDDocument pdDocument = getPDDocument();
        PageDrawer.Builder builder = new PageDrawer.Builder(pdDocument, wordDrawStyle);
        PDFRenderer pdfRenderer = new PDFRenderer(document.getPdDocument());

        for (Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext(); ) {
            Page page = pages.next();
            int pageIndex = page.getIndex();
            PageDrawer drawer = builder.createPageDrawer(pageIndex);
            for (Iterator<TextChunk> words = page.getWords(); words.hasNext(); ) {
                drawWord(drawer, words.next());
            }
            drawer.close();
        }

        String outFilePath = getOutputFilePath(wordDirectoryName, wordFileNameSuffix);
        pdDocument.save(outFilePath);
        pdDocument.close();
    }

    private void drawWord(PageDrawer drawer, TextChunk word) throws IOException {
        drawer.drawRectangle(word.getLeft(), word.getTop(), word.getRight(), word.getBottom());
        String s = String.valueOf(word.getStartOrder());
        //String s = String.valueOf(word.getCoherence());
        drawer.drawString(s, word.getLeft(), word.getTop());
        drawer.drawLine(word.getRight(), word.getBottom(), word.getRight() + word.getSpaceWidth(), word.getBottom());
    }

    private void drawBlocks() throws IOException {
        PDDocument pdDocument = getPDDocument();
        PageDrawer.Builder builder = new PageDrawer.Builder(pdDocument, blockDrawStyle);

        for (Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext(); ) {
            Page page = pages.next();
            int pageIndex = page.getIndex();

            PageDrawer drawer = builder.createPageDrawer(pageIndex);
            for (Iterator<TextChunk> blocks = page.getBlocks(); blocks.hasNext(); ) {
                drawBlock(drawer, blocks.next());
            }
            drawer.close();
        }

        String outFilePath = getOutputFilePath(blockDirectoryName, blockFileNameSuffix);
        pdDocument.save(outFilePath);
        pdDocument.close();
    }

    private void drawBlock(PageDrawer drawer, TextChunk block) throws IOException {
        drawer.drawRectangle(block.getLeft(), block.getTop(), block.getRight(), block.getBottom());
        String s = String.valueOf(block.getStartOrder());
        String font_size = String.valueOf(block.getFont().getFontSize());
        drawer.drawString(s, block.getRight(), block.getBottom());
        drawer.drawString("fs: " + font_size, block.getLeft(), block.getTop());
    }

    private void drawTextLines() throws IOException {
        PDDocument pdDocument = getPDDocument();
        PageDrawer.Builder builder = new PageDrawer.Builder(pdDocument, blockDrawStyle);

        for (Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext(); ) {
            Page page = pages.next();
            int pageIndex = page.getIndex();

            PageDrawer drawer = builder.createPageDrawer(pageIndex);
            for (TextChunk line: page.getTextLines()) {
                drawTextLine(drawer, line);
            }
            drawer.close();
        }

        String outFilePath = getOutputFilePath(textLinesDirectoryName, textLinesFileNameSuffix);
        pdDocument.save(outFilePath);
        pdDocument.close();
    }

    private void drawTextLine(PageDrawer drawer, TextChunk block) throws IOException {
        drawer.drawRectangle(block.getLeft(), block.getTop(), block.getRight(), block.getBottom());
        String s = String.valueOf(block.getStartOrder());
        //String font_size = String.valueOf(block.getFont().getFontSize());
        drawer.drawString(s, block.getRight(), block.getBottom());
        //drawer.drawString("fs: " + font_size, block.getLeft(), block.getTop());
    }

    private void drawProjections() throws IOException {
        PDDocument pdDocument = getPDDocument();
        PageDrawer.Builder builder = new PageDrawer.Builder(pdDocument, rulingDrawStyle);

        for (Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext(); ) {
            Page page = pages.next();
            int pageIndex = page.getIndex();
            PageDrawer drawer = builder.createPageDrawer(pageIndex);
            for (Table table: page.getTables()) {
                for (Range h: table.getHorizontal()) {
                    drawHorizontal(drawer, h, table);
                }
                //table.getHorizontal();
                //table.getVertical();
            }
            drawer.close();
        }

        String outFilePath = getOutputFilePath(rulingDirectoryName, rulingFileNameSuffix);
        pdDocument.save(outFilePath);
        pdDocument.close();
    }

    private void drawHorizontal(PageDrawer drawer, Range range, Table table) throws IOException {
        double x1 = range.getStart();
        double y1 = table.getBottom();
        double x2 = range.getEnd();
        double y2 = table.getBottom();
        drawer.drawLine(x1, y1, x2, y2);
    }

    private void drawRulings() throws IOException {

        PDDocument pdDocument = getPDDocument();
        DrawStyle ds4 = new DrawStyle.Builder().setStrokingColor(Color.ORANGE).setLineWidth(3f).createDrawStyle();
        PageDrawer.Builder builder4 = new PageDrawer.Builder(pdDocument, ds4);

        for (Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext(); ) {
            Page page = pages.next();
            int pageIndex = page.getIndex();
            PageDrawer drawer = builder4.createPageDrawer(pageIndex);
            for (Ruling r: page.getListRulings()) {
                drawRuling(drawer, r);
            }
            drawer.close();
        }

        String outFilePath = getOutputFilePath(rulingDirectoryName, rulingFileNameSuffix);
        pdDocument.save(outFilePath);
        pdDocument.close();
    }

    private void drawRuling(PageDrawer drawer, Ruling ruling) throws IOException {
        float x1 = ruling.x1;
        float y1 = ruling.y1;
        float x2 = ruling.x2;
        float y2 = ruling.y2;
        drawer.drawRectangle(x1, y1, x2, y2);
    }

   /* private void drawSections() throws IOException {
        PDDocument pdDocument = getPDDocument();
        PageDrawer.Builder builder = new PageDrawer.Builder(pdDocument, sectionDrawStyle);

        for (Iterator<Page> pages = document.getPages(); pages.hasNext(); ) {
            Page page = pages.next();
            int pageIndex = page.getIndex();

            PageDrawer drawer = builder.createPageDrawer(pageIndex);
            for (Iterator<Section> sections = page.getSections(); sections.hasNext(); ) {
                Section section = sections.next();
                if (pageIndex == section.getStartPage()) {
                    String text = String.format("%s: START", section.getName());
                    drawer.drawString(text, 20, section.getTop());
                }
                if (pageIndex == section.getEndPage()) {
                    String text = String.format("%s: END", section.getName());
                    drawer.drawString(text, 20, section.getBottom());
                }
            }
            drawer.close();
        }

        String outFilePath = getOutputFilePath(sectionDirectoryName, sectionFileNameSuffix);
        pdDocument.save(outFilePath);
        pdDocument.close();
    }*/

    private void drawBorderedTables() throws IOException {

        double lt, rt, tp, bm;
        PDDocument pdDocument = getPDDocument();
        //PDDocument pdDocument = PDDocument.load(new File(getOutputFilePath(recomposedDocumentDirectoryName, recomposedDocumentNameSuffix)));

        DrawStyle ds1 = new DrawStyle.Builder().setStrokingColor(GREEN).setLineWidth(3f).createDrawStyle();
        PageDrawer.Builder builder1 = new PageDrawer.Builder(pdDocument, ds1);

        DrawStyle ds2 = new DrawStyle.Builder().setLineWidth(3f).setStrokingColor(BLUE).createDrawStyle();
        PageDrawer.Builder builder2 = new PageDrawer.Builder(pdDocument, ds2);

        DrawStyle ds3 = new DrawStyle.Builder().setStrokingColor(RED).setLineWidth(1f).createDrawStyle();
        PageDrawer.Builder builder3 = new PageDrawer.Builder(pdDocument, ds3);

        DrawStyle ds4 = new DrawStyle.Builder().setStrokingColor(Color.ORANGE).setLineWidth(3f).createDrawStyle();
        PageDrawer.Builder builder4 = new PageDrawer.Builder(pdDocument, ds4);

        for (Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext(); ) {
            Page page = pages.next();

            PageDrawer drawer3 = builder1.createPageDrawer(page.getIndex());
            for (Iterator<PDFRectangle> tableIterator = page.getPossibleTableArea(); tableIterator.hasNext(); ) {
                PDFRectangle table = tableIterator.next();
                lt = table.getLeft();
                tp = table.getTop();
                rt = table.getRight();
                bm = table.getBottom();
                drawer3.drawRectangle(lt - 2, bm + 2, rt + 2, tp - 2);
            }

            drawer3.close();

            drawer3 = builder4.createPageDrawer(page.getIndex());
            for (Iterator<Table> tableIterator = page.getTables().iterator(); tableIterator.hasNext(); ) {
                PDFRectangle table = tableIterator.next();
                lt = table.getLeft();
                tp = table.getTop();
                rt = table.getRight();
                bm = table.getBottom();
                drawer3.drawRectangle(lt - 2, tp - 2, rt + 2, bm + 2);
            }

            drawer3.close();

            drawer3 = builder2.createPageDrawer(page.getIndex());
            List<Ruling> rectangleList = page.getJoinedRulings();
            RectangleComparator comp = new RectangleComparator();
            for (Ruling r: rectangleList) {
                drawer3.drawLine(r.x1, r.y1, r.x2, r.y2);
            }

/*            for (Rectangle2D rec: page.getFrames()){
                drawer3.drawRectangle(rec.getX(), rec.getY(), rec.getX() - rec.getWidth(), rec.getY() - rec.getHeight());
            }*/

            //Collections.sort(rectangleList, comp);
  /*          if (rectangleList.size() > 0) {
                PDFRectangle rec = rectangleList.get(0);
                lt = rec.getX();
                tp = rec.getY();
                rt = rec.getX() + rec.getWidth();
                bm = rec.getY() + rec.getHeight();
                drawer3.drawRectangle(lt, tp, rt, bm);
            }*/
            drawer3.close();
        }

        for (Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext(); ) {
            Page page = pages.next();
            PageDrawer drawer3 = builder3.createPageDrawer(page.getIndex());
            for (Iterator<Table> tableIterator = page.getTables().iterator(); tableIterator.hasNext(); ) {
                Table table = tableIterator.next();
                Iterator<Cell> pdfRectangleIterator = table.getCells();
                while(pdfRectangleIterator.hasNext()){
                    PDFRectangle c = pdfRectangleIterator.next();
                    lt = c.getLeft();
                    tp = c.getTop();
                    rt = c.getRight();
                    bm = c.getBottom();
                    drawer3.drawRectangle(lt + 3, tp - 3, rt - 3, bm + 3);
                }

            }

            drawer3.close();
        }


        String outFilePath = getOutputFilePath(borderedTableDirectoryName, borderedTableFileNameSuffix);
        pdDocument.save(outFilePath);
        pdDocument.close();
    }

    private String getOutputFilePath(String innerDirectoryName, String fileNameSuffix) {
        // Make the specified output directory
        if (null == innerDirectoryName) {
            innerDirectoryName = "";
        } else {
            innerDirectoryName = File.separator.concat(innerDirectoryName);
        }
        String outputDirectoryPath = debugDirectoryPath.toString();
        String outDirectoryPath = outputDirectoryPath.concat(innerDirectoryName);
        File outDirectory = new File(outDirectoryPath);

        outDirectory.mkdirs();

        // Build the output file path
        String suffix = "";
        if (null != fileNameSuffix && !fileNameSuffix.isEmpty())
            suffix = SUFFIX_START_CHAR.concat(fileNameSuffix);

        File file = document.getSourceFile();
        String fileName = FilenameUtils.removeExtension(file.getName());
        return String.format("%s/%s%s.pdf", outDirectoryPath, fileName, suffix);
    }

    public static class Builder {
        private Path debugDirectoryPath;

        private String chunkDirectoryName;
        private String charDirectoryName;
        private String wordDirectoryName;
        private String blockDirectoryName;
        private String rulingDirectoryName;
        private String sectionDirectoryName;
        private String workAreaDirectoryName;
        private String textLinesDirectoryName;
        private String borderedTableDirectoryName;
        private String nakedTableDirectoryName;
        private String allTableDirectoryName;
        private String multipageWorkAreasDirectoryName;
        private String recomposedDocumentDirectoryName;

        private String chunkFileNameSuffix;
        private String charFileNameSuffix;
        private String wordFileNameSuffix;
        private String blockFileNameSuffix;
        private String rulingFileNameSuffix;
        private String textLinesFileNameSuffix;

        private String sectionFileNameSuffix;
        private String workAreaFileNameSuffix;
        private String borderedTableFileNameSuffix;
        private String nakedTableFileNameSuffix;
        private String allTableFileNameSuffix;
        private String multipageWorkAreasFileNameSuffix;
        private String recomposedDocumentNameSuffix;

        private DrawStyle chunkDrawStyle;
        private DrawStyle charDrawStyle;
        private DrawStyle wordDrawStyle;
        private DrawStyle blockDrawStyle;
        private DrawStyle rulingDrawStyle;
        private DrawStyle sectionDrawStyle;
        private DrawStyle workAreaDrawStyle;

        // Default settings
        {
            DrawStyle.Builder builder = new DrawStyle.Builder()
                    .setStrokingColor(Color.BLACK)
                    .setNonStrokingColor(Color.RED)
                    .setLineWidth(0.25f)
                    .setFont(PDType1Font.HELVETICA_BOLD)
                    .setFontSize(6f);

            chunkDrawStyle = builder.createDrawStyle();
            charDrawStyle = builder.createDrawStyle();
            wordDrawStyle = builder.createDrawStyle();
            blockDrawStyle = builder.createDrawStyle();

            builder.setFontSize(12f);
            sectionDrawStyle = builder.createDrawStyle();

            builder.setStrokingColor(Color.RED).setLineWidth(1f);
            workAreaDrawStyle = builder.createDrawStyle();

            builder.setStrokingColor(Color.BLUE).setLineWidth(3.5f);
            rulingDrawStyle = builder.createDrawStyle();
        }

        public Builder(Path debugDirectoryPath) {
            setDebugDirectoryPath(debugDirectoryPath);
        }

        private Builder setDebugDirectoryPath(Path path) {
            if (null == path) {
                throw new IllegalArgumentException("The path to a debug directory cannot be null");
            } else {
                this.debugDirectoryPath = path;
                return this;
            }
        }

        public Builder setChunkDirectoryName(String directoryName) {
            chunkDirectoryName = directoryName;
            return this;
        }

        public Builder setCharDirectoryName(String directoryName) {
            charDirectoryName = directoryName;
            return this;
        }

        public Builder setTextLinesDirectoryName(String directoryName) {
            textLinesDirectoryName = directoryName;
            return this;
        }

        public Builder setWordDirectoryName(String directoryName) {
            wordDirectoryName = directoryName;
            return this;
        }

        public Builder setBlockDirectoryName(String directoryName) {
            blockDirectoryName = directoryName;
            return this;
        }

        public Builder setRulingDirectoryName(String directoryName) {
            rulingDirectoryName = directoryName;
            return this;
        }

        public Builder setSectionDirectoryName(String directoryName) {
            sectionDirectoryName = directoryName;
            return this;
        }

        public Builder setWorkAreaDirectoryName(String directoryName) {
            workAreaDirectoryName = directoryName;
            return this;
        }

        public Builder setBorderedTableDirectoryName(String directoryName) {
            borderedTableDirectoryName = directoryName;
            return this;
        }

        public Builder setNakedTableDirectoryName(String directoryName) {
            nakedTableDirectoryName = directoryName;
            return this;
        }

        public Builder setAllTableDirectoryName(String directoryName) {
            allTableDirectoryName = directoryName;
            return this;
        }

        public Builder setMultipageWorkAreasDirectoryName(String directoryName) {
            multipageWorkAreasDirectoryName = directoryName;
            return this;
        }

        public Builder setRecomposedDocumentDirectoryName(String directoryName) {
            recomposedDocumentDirectoryName = directoryName;
            return this;
        }

        public Builder setChunkFileNameSuffix(String fileNameSuffix) {
            chunkFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setTextLinesFileNameSuffix(String fileNameSuffix) {
            textLinesFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setCharFileNameSuffix(String fileNameSuffix) {
            charFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setWordFileNameSuffix(String fileNameSuffix) {
            wordFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setBlockFileNameSuffix(String fileNameSuffix) {
            blockFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setRulingFileNameSuffix(String fileNameSuffix) {
            rulingFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setSectionFileNameSuffix(String fileNameSuffix) {
            sectionFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setWorkAreaFileNameSuffix(String fileNameSuffix) {
            workAreaFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setBorderedTableFileNameSuffix(String fileNameSuffix) {
            borderedTableFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setNakedTableFileNameSuffix(String fileNameSuffix) {
            nakedTableFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setAllTableFileNameSuffix(String fileNameSuffix) {
            allTableFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setMultipageWorkAreasFileNameSuffix(String fileNameSuffix) {
            multipageWorkAreasFileNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setRecomposedDocumentNameSuffix(String fileNameSuffix) {
            recomposedDocumentNameSuffix = fileNameSuffix;
            return this;
        }

        public Builder setChunkDrawStyle(DrawStyle drawStyle) {
            chunkDrawStyle = drawStyle;
            return this;
        }

        public Builder setCharDrawStyle(DrawStyle drawStyle) {
            charDrawStyle = drawStyle;
            return this;
        }

        public Builder setWordDrawStyle(DrawStyle drawStyle) {
            wordDrawStyle = drawStyle;
            return this;
        }

        public Builder setBlockDrawStyle(DrawStyle drawStyle) {
            blockDrawStyle = drawStyle;
            return this;
        }

        public Builder setRulingDrawStyle(DrawStyle drawStyle) {
            rulingDrawStyle = drawStyle;
            return this;
        }

        public Builder setSectionDrawStyle(DrawStyle drawStyle) {
            sectionDrawStyle = drawStyle;
            return this;
        }

        public Builder setWorkAreaDrawStyle(DrawStyle drawStyle) {
            workAreaDrawStyle = drawStyle;
            return this;
        }

        public DebugDrawer createDebugDrawer(Document document) {
            DebugDrawer drawer = new DebugDrawer(document, debugDirectoryPath);

            drawer.chunkDirectoryName = this.chunkDirectoryName;
            drawer.charDirectoryName = this.charDirectoryName;
            drawer.wordDirectoryName = this.wordDirectoryName;
            drawer.blockDirectoryName = this.blockDirectoryName;
            drawer.rulingDirectoryName = this.rulingDirectoryName;
            drawer.borderedTableDirectoryName = this.borderedTableDirectoryName;
            drawer.recomposedDocumentDirectoryName = this.recomposedDocumentDirectoryName;
            drawer.textLinesDirectoryName = this.textLinesDirectoryName;

            drawer.chunkFileNameSuffix = this.chunkFileNameSuffix;
            drawer.charFileNameSuffix = this.charFileNameSuffix;
            drawer.wordFileNameSuffix = this.wordFileNameSuffix;
            drawer.blockFileNameSuffix = this.blockFileNameSuffix;
            drawer.rulingFileNameSuffix = this.rulingFileNameSuffix;
            drawer.borderedTableFileNameSuffix = this.borderedTableFileNameSuffix;
            drawer.recomposedDocumentNameSuffix = this.recomposedDocumentNameSuffix;
            drawer.textLinesFileNameSuffix = this.textLinesFileNameSuffix;

            drawer.chunkDrawStyle = this.chunkDrawStyle;
            drawer.charDrawStyle = this.charDrawStyle;
            drawer.wordDrawStyle = this.wordDrawStyle;
            drawer.blockDrawStyle = this.blockDrawStyle;
            drawer.rulingDrawStyle = this.rulingDrawStyle;

            return drawer;
        }
    }

}
