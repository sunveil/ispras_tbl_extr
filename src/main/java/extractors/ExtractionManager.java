package extractors;

import extractors.bordered.BorderedTableExtractor;
import model.Document;
import model.Page;
import model.table.Table;
import model.table.TableType;
import org.apache.commons.io.FilenameUtils;

import java.util.*;

public final class ExtractionManager {
    private Document document;


    public ExtractionManager(Document document) {

        this.document = document;
    }

    public List<Table> extract() {
        List<Table> result = new ArrayList<>();

        String docFileName = document.getSourceFile().getName();

        for(Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext();) {
            Page page = pages.next();
            BorderedTableExtractor bte = new BorderedTableExtractor(page);
            List<Table> borderedTables = bte.extract();
            int ordinal = 1;

            // Code tables
            if (null != borderedTables) {
                codeTables(borderedTables, docFileName, ordinal, page.getIndex(), "BR");
                codeBorderedTRables(borderedTables, docFileName, ordinal, page.getIndex());
                result.addAll(borderedTables);
            }

        }

         return result.isEmpty() ? null : result;
    }

    public List<Table> extract(int startPage, int endPage) {
        List<Table> result = new ArrayList<>();

        String docFileName = document.getSourceFile().getName();

        for(int i = startPage; i <= endPage; i++) {
            Page page = document.getPage(i);
            BorderedTableExtractor bte = new BorderedTableExtractor(page);
            List<Table> borderedTables = bte.extract();
            int ordinal = 1;

            // Code tables
            if (null != borderedTables) {
                codeTables(borderedTables, docFileName, ordinal, page.getIndex(), "BR");
                codeBorderedTRables(borderedTables, docFileName, ordinal, page.getIndex());
                result.addAll(borderedTables);
            }

        }

        return result.isEmpty() ? null : result;
    }

    private void codeBorderedTRables(List<Table> tables, String fileName, int section, int pageIndex) {
        if (null == tables || tables.isEmpty())
            return;

        fileName = FilenameUtils.getBaseName(fileName);

        for (int i = 0; i < tables.size(); i++) {
            Table table  = tables.get(i);
            String suffix;
            if (table.getType() == TableType.FULL_BORDERED)
                suffix = "BR";
            else
            if (table.getType() == TableType.PARTIAL_BORDERED)
                suffix = "PB";
            else
                suffix = "UN";

            String code = String.format("%s_S%02d_P%03d_T%03d%s", fileName, section, pageIndex + 1, i + 1, suffix);
            table.setCode(code);
        }

    }

    private void codeTables(List<Table> tables, String fileName, int section, int pageIndex, String suffix) {
        if (null == tables || tables.isEmpty()) return;

        fileName = FilenameUtils.getBaseName(fileName);

        for (int i = 0; i < tables.size(); i++) {
            Table table  = tables.get(i);
            String code = String.format("%s_S%02d_P%03d_T%03d%s", fileName, section, pageIndex + 1, i + 1, suffix);
            table.setCode(code);
        }
    }

}
