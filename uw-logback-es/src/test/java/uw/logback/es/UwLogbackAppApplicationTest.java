package uw.logback.es;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;

/**
 * 测试日志输出
 *
 * 
 * @since 2018-07-25
 */
public class UwLogbackAppApplicationTest {

    private static final Logger logger = LoggerFactory.getLogger(UwLogbackAppApplicationTest.class);

    @Test
    public void testLogger() {
        logger.error("test");
        try {
            throw new RuntimeException("RuntimeException message");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void testJsonRaw() throws IOException {
        MappingJsonFactory jsonFactory = (MappingJsonFactory) new ObjectMapper()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .getFactory()
                .disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);
        StringWriter sw = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(sw);
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("tes", "just for \"test!");
        jsonGenerator.flush();
        System.out.println(sw);
    }
}
