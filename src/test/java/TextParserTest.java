import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.io.File;
import java.io.IOException;


public class TextParserTest {

    @InjectMocks
    private DedocTableExtractor dedocTableExtractor;

    @Test
    public void testMainMethodWithInput() throws IOException {
        String[] args = {
                "-i", "./data/Document635.pdf",
                "-o", "./data/out",
                "-tmp", "./data/out"
        };

        dedocTableExtractor.main(args);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(new File("./data/outdata.json"));
        Assertions.assertEquals(node.get("document").asText(), "Document635.pdf");
    }


}
