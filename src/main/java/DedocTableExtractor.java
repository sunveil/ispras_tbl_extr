import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import debug.DebugDrawer;
import exceptions.EmptyArgumentException;
import extractors.BlockComposer;
import extractors.ExtractionManager;
import model.Document;
import model.Page;
import model.table.Table;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import utils.Config;
import writers.HtmlTableWriter;
import writers.JaksonWriter;
import writers.JsonDocumentWriter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;


public class DedocTableExtractor {
    static {
        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");
    }
    // CLI params
    @Option(name = "-i", aliases = {"--input"}, required = true, metaVar = "PATH", usage = "specify a file")
    private String inArg;
    private File inputFile;
    private Path inputPath;

    @Option(name = "-o", aliases = {"--output"}, metaVar = "PATH", usage = "Specify a directory for extracted data")
    private String outArg;
    private File outputFile;
    private Path outputPath;

    @Option(name = "-sp", aliases = {"--start"}, usage = "Specify a start page")
    private String sPage;
    private int startPage;

    @Option(name = "-ep", aliases = {"--end"}, usage = "Specify a end page")
    private String ePage;
    private int endPage;

    @Option(name = "-d", aliases = {"--debug"}, usage = "Allow debug output")
    private boolean debug = false;

    @Option(name = "-rf", aliases = {"--remove frame"}, usage = "Remove GOST frame")
    private String rf = "";

    @Option(name = "-tmp", aliases = {"--temporary"}, usage = "temporary directory")
    private String tmpDir = "";

    @Option(name = "-?", aliases = {"--help"}, usage = "Show this message")
    private boolean help = false;

    public static void main(String[] args) {
        System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog","fatal");
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        new DedocTableExtractor().run(args);
    }

    public void run(String[] args) {

        Config config = new Config();
        Map<Integer, Rectangle2D.Float> GOSTFrames = new HashMap<>();

        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);

            if (help) {
                parser.printUsage(System.err);
                System.exit(0);
            }

            throwIfEmpty(inArg);

            if (!rf.isEmpty()){
                Config.removeFrame = true;
                Config.pathToGOSTJson = rf;
            }

            inputFile = new File(inArg);
            inputPath = inputFile.isFile() ? inputFile.getParentFile().toPath() : inputFile.toPath();

            if (isEmptyArg(outArg)) {
                outputFile = inputPath.resolve("output").toFile();
            } else {
                outputFile = new File(outArg);
            }

            outputFile.mkdirs();
            outputPath = outputFile.toPath();

            if (isEmptyArg(tmpDir)) {
                Config.tmpDir = outputFile.getParent()+"/";
            } else {
                Config.tmpDir = tmpDir;
            }

            File tmpDir = new File(Config.tmpDir);
            if (tmpDir.exists() && tmpDir.isDirectory()) {
            } else {
                tmpDir.mkdir();
            }

            if (inputFile.isFile()) {
                if (Config.removeFrame) {
                    GOSTFrames = readGOSTJson(Config.pathToGOSTJson);
                    Map<Integer, Rectangle2D> bboxes = new HashMap<>();
                    PDDocument doc = Loader.loadPDF(inputFile);
                    for (int i=0; i < doc.getPages().getCount(); i++){
                        PDPage page = doc.getPage(i);
                        Rectangle2D.Float rec = GOSTFrames.get(i);
                        if (rec == null) continue;
                        float w = (float) (rec.getWidth() * page.getCropBox().getWidth() );
                        float h = (float) (rec.getHeight() * page.getCropBox().getHeight());
                        float x = (float) (rec.getX() * page.getCropBox().getWidth());
                        //float y = (float) (page.getCropBox().getHeight() - ((rec.getY() * page.getCropBox().getHeight()) + h));
                        float y = (float) (rec.getY() * page.getCropBox().getHeight());
                        Rectangle2D.Float newRec = new Rectangle2D.Float(x,y,w,h);
                        bboxes.put(i, newRec);
/*                        if (newRec != null) {
                            page.setTrimBox(newRec);
                            page.setCropBox(newRec);
                            page.setMediaBox(newRec);
                            page.setBleedBox(newRec);
                            page.setArtBox(newRec);
                        }*/
                    }
                    Config.bboxes = bboxes;
/*                    File cropped = new File(Config.tmpDir+"/cropped_"+inputFile.getName());
                    doc.save(cropped);
                    doc.close();
                    inputFile = cropped;*/
                }
                if (sPage != null && !sPage.isEmpty() && ePage != null && !ePage.isEmpty()) {
                    startPage = Integer.parseInt(sPage);
                    endPage = Integer.parseInt(ePage);
                    extract(inputFile.toPath(), startPage - 1, endPage - 1, GOSTFrames);
                } else {
                    extract(inputFile.toPath(), GOSTFrames);
                }
            } else {
                final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.{pdf,PDF}");
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(inputPath, "*.pdf")) {
                    for (Path file : directoryStream) {
                        if (matcher.matches(file.getFileName())) {
                            extract(file, GOSTFrames);
                        }
                    }
                }
                catch (IOException | DirectoryIteratorException ex) {
                    ex.printStackTrace();
                }
            }

        } catch (CmdLineException | IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, Rectangle2D.Float> readGOSTJson(String path) throws IOException {
        Map<Integer, Rectangle2D.Float> GOSTFrames = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        File json = new File(path);
        JsonNode root = mapper.readTree(json);

        for (Iterator<Map.Entry<String, JsonNode>> fields = root.fields(); fields.hasNext();) {
            Map.Entry<String, JsonNode> field = fields.next();
            int pageIndex = Integer.parseInt(field.getKey());
            float width = field.getValue().get("width").asInt();
            float height = field.getValue().get("height").asInt();
            float originalImageWidth = field.getValue().get("original_image_width").asInt();
            float originalImageHeight = field.getValue().get("original_image_height").asInt();
            float left = field.getValue().get("x_top_left").asInt();
            float top =  field.getValue().get("y_top_left").asInt();
            GOSTFrames.put(pageIndex, new Rectangle2D.Float(
                    left/originalImageWidth,
                    top/originalImageHeight,
                    width/originalImageWidth,
                    height/originalImageHeight));
        }
        return GOSTFrames;
    }

    public void extract(Path path, int startPage, int endPage, Map<Integer, Rectangle2D.Float> GOSTFrames) throws IOException, ParserConfigurationException, TransformerException {

        Document document = null;
        document = Document.load(path, startPage, endPage);
        document.setBBoxes(GOSTFrames);

        int lastPageIndex = document.getPageCnt();
        if (startPage > lastPageIndex)
            return;
        startPage = startPage <= 0 ? 0: startPage;
        endPage = endPage >= lastPageIndex ? lastPageIndex : endPage;
        BlockComposer bc = new BlockComposer();
        bc.compose(document, startPage, endPage);

        ExtractionManager em = new ExtractionManager(document);
        List<Table> tables = em.extract(startPage, endPage);

        printJSON(document);

        if (debug) {
            writeTables(document);
            drawDebug(document);
        }
    }

    public void extract(Path path, Map<Integer, Rectangle2D.Float> GOSTFrames) throws IOException, ParserConfigurationException, TransformerException {
        Document document = null;
        PDDocument pdDocument = Loader.loadPDF(path.toFile());

        document = Document.load(path, 0, pdDocument.getPages().getCount() - 1);

        BlockComposer bc = new BlockComposer();
        bc.compose(document);

        ExtractionManager em = new ExtractionManager(document);
        List<Table> tables = em.extract();

        printJSON(document);

        if (debug) {
            drawDebug(document);
            writeTables(document);
        }
    }

    private void drawDebug(Document document) throws IOException, ParserConfigurationException, TransformerException {
        DebugDrawer debugDrawer = null;
        Path debugDirPath = outputPath.resolve("debug");

        DebugDrawer.Builder debugDrawerBuilder = new DebugDrawer.Builder(debugDirPath)
                .setChunkDirectoryName("Chunks")
                .setChunkFileNameSuffix("CHUNKS")
                .setCharDirectoryName("Chars")
                .setCharFileNameSuffix("CHARS")
                .setWordDirectoryName("Words")
                .setWordFileNameSuffix("WORDS")
                .setBlockDirectoryName("Blocks")
                .setBlockFileNameSuffix("BLOCKS")
                .setRulingDirectoryName("Rulings")
                .setRulingFileNameSuffix("RULINGS")
                .setBorderedTableDirectoryName("BorderedTables")
                .setBorderedTableFileNameSuffix("BORDERED")
                .setTextLinesDirectoryName("TextLines")
                .setTextLinesFileNameSuffix("TEXTLINES");

        debugDrawer = debugDrawerBuilder.createDebugDrawer(document);
        debugDrawer.drawBeforeRecomposing();
    }

    private void printJSON(Document document) throws IOException {
        //JsonDocumentWriter writer = new JsonDocumentWriter(document);
        JaksonWriter jaksonWriter = new JaksonWriter(document);
        jaksonWriter.write();
    }

    private void printJSON(Document document, int startPage, int endPage) throws IOException {
        //JsonDocumentWriter writer = new JsonDocumentWriter(document, startPage, endPage);
        JaksonWriter jaksonWriter = new JaksonWriter(document, startPage, endPage);
        jaksonWriter.write();
    }

    private void printTables(Document document) throws IOException {
        Path output = outputPath.resolve("tables");
        Files.createDirectories(output);

        HtmlTableWriter writer = new HtmlTableWriter();
        StringBuilder html = new StringBuilder();

        final String fileName = FilenameUtils.getBaseName(document.getSourceFile().getName());

        html.append("<!DOCTYPE html>").append(System.lineSeparator());
        html.append("<html>").append(System.lineSeparator());

        // Write head
        html.append("<head>").append(System.lineSeparator());
        html.append("<meta charset=\"UTF-8\">").append(System.lineSeparator());
        html.append("<title>").append(fileName).append("</title>").append(System.lineSeparator());
        html.append("</head>").append(System.lineSeparator());

        // Write body
        html.append("<body>").append(System.lineSeparator());

        try {
            Iterator<Page> pages = document.getPagesItrerator();
            while (pages.hasNext()) {
                Page page = pages.next();
                html.append("<h1>Page ").append(page.getIndex()).append("</h1>").append(System.lineSeparator());

                List<Table> tables = page.getTables();
                if (null == tables || tables.isEmpty())
                    continue;
                tables.sort(Comparator.comparing(Table::getPageIndex).thenComparing(Table::getTop));
                for (Table table: tables) {
                    table.splitCells();
                    table.completeRows();
                    table.removeEmptyRows();
                    if (table.isContinued())
                        continue;
                    else {
                        html.append("<p>").append(System.lineSeparator());
                        html.append(writer.write(table));
                        html.append("</p>").append(System.lineSeparator());
                    }
                }
            }

            html.append("</body>").append(System.lineSeparator());
            html.append("</html>");

            System.out.println(html.toString());

        }
        catch (ParserConfigurationException | TransformerException | IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTables(Document document) throws IOException {
        Path output = outputPath.resolve("tables");
        Files.createDirectories(output);

        HtmlTableWriter writer = new HtmlTableWriter();
        StringBuilder html = new StringBuilder();

        final String fileName = FilenameUtils.getBaseName(document.getSourceFile().getName());

        html.append("<!DOCTYPE html>").append(System.lineSeparator());
        html.append("<html>").append(System.lineSeparator());

        // Write head
        html.append("<head>").append(System.lineSeparator());
        html.append("<meta charset=\"UTF-8\">").append(System.lineSeparator());
        html.append("<title>").append(fileName).append("</title>").append(System.lineSeparator());
        html.append("</head>").append(System.lineSeparator());

        // Write body
        html.append("<body>").append(System.lineSeparator());

        try {
            Iterator<Page> pages = document.getPagesItrerator();
            while (pages.hasNext()) {
                Page page = pages.next();
                html.append("<h1>Page ").append(page.getIndex()).append("</h1>").append(System.lineSeparator());

                List<Table> tables = page.getTables();
                if (null == tables || tables.isEmpty())
                    continue;
                tables.sort(Comparator.comparing(Table::getPageIndex).thenComparing(Table::getTop));
                for (Table table: tables) {
                    if (table.isContinued())
                        continue;
                    else {
                        html.append("<p>").append(System.lineSeparator());
                        html.append(writer.write(table));
                        html.append("</p>").append(System.lineSeparator());
                    }
                }
            }

            html.append("</body>").append(System.lineSeparator());
            html.append("</html>");

            String fn = String.format("%s.html", fileName);
            Path p = output.resolve(fn);

            Writer fileWriter = new OutputStreamWriter(new FileOutputStream(p.toFile()), StandardCharsets.UTF_8);
            fileWriter.write(html.toString());
            fileWriter.close();
        }
        catch (ParserConfigurationException | TransformerException | IOException e) {
            e.printStackTrace();
        }
    }

    private void throwIfEmpty(String arg) {
        if (isEmptyArg(arg)) {
            throw new EmptyArgumentException("A required option was not specified");
        }
    }

    private boolean isEmptyArg(String arg) {
        return arg == null || arg.isEmpty();
    }



}
