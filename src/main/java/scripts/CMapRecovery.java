package scripts;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.PrintWriter;

public class CMapRecovery {
/*
    public static void main(String[] args){
        try (PDDocument doc = PDDocument.load(f))
        {
            for (int p = 0; p < doc.getNumberOfPages(); ++p)
            {
                PDPage page = doc.getPage(p);
                PDResources res = page.getResources();
                for (COSName fontName : res.getFontNames())
                {
                    PDFont font = res.getFont(fontName);
                    COSBase encoding = font.getCOSObject().getDictionaryObject(COSName.ENCODING);
                    if (!COSName.IDENTITY_H.equals(encoding))
                    {
                        continue;
                    }
                    // get real name
                    String fname = font.getName();
                    int plus = fname.indexOf('+');
                    if (plus != -1)
                    {
                        fname = fname.substring(plus + 1);
                    }
                    if (font.getCOSObject().containsKey(COSName.TO_UNICODE))
                    {
                        continue;
                    }
                    System.out.println("File '" + f.getName() + "', page " + (p + 1) + ", " + fontName.getName() + ", " + font.getName());
                    if (!fname.startsWith("Calibri-Bold"))
                    {
                        continue;
                    }
                    COSStream toUnicodeStream = new COSStream();
                    try (PrintWriter pw = new PrintWriter(toUnicodeStream.createOutputStream(COSName.FLATE_DECODE)))
                    {
                        // "9.10 Extraction of Text Content" in the PDF 32000 specification
                        pw.println ("/CIDInit /ProcSet findresource begin\n" +
                                "12 dict begin\n" +
                                "begincmap\n" +
                                "/CIDSystemInfo\n" +
                                "<< /Registry (Adobe)\n" +
                                "/Ordering (UCS) /Supplement 0 >> def\n" +
                                "/CMapName /Adobe-Identity-UCS def\n" +
                                "/CMapType 2 def\n" +
                                "1 begincodespacerange\n" +
                                "<0000> <FFFF>\n" +
                                "endcodespacerange\n" +
                                "10 beginbfchar\n" + // number is count of entries
                                "<0001><0020>\n" + // space
                                "<0002><0041>\n" + // A
                                "<0003><0042>\n" + // B
                                "<0004><0044>\n" + // D
                                "<0013><0065>\n" + // e
                                "<0012><0064>\n" + // d
                                "<0017><0069>\n" + // i
                                "<001B><006E>\n" + // n
                                "<0015><0067>\n" + // g
                                "<0020><0075>\n" + // u
                                "endbfchar\n" +
                                "endcmap CMapName currentdict /CMap defineresource pop end end");
                    }
                    font.getCOSObject().setItem(COSName.TO_UNICODE, toUnicodeStream);
                }
            }
            doc.save("huhu.pdf");
        }
    }*/

}
