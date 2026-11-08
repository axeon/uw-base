package uw.common.vo;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PropertyBox 单测：四源工厂构造（字符串/流/文件/Properties）、空输入与失败降级 EMPTY、
 * 强类型取值族的默认值语义（缺失/空白/畸形一律默认值不抛异常）、getProperties 原始暴露。
 */
class PropertyBoxTest {

    /**
     * 标准端口映射样例（ops 系 DB 列的典型形态）。
     */
    private static final String PORTS_TEXT = "amqp=5676\nmgmt=15676\ndist=25676\n";

    @Test
    void loadFromStringParsesKeyValueLines() {
        PropertyBox box = PropertyBox.loadFromString(PORTS_TEXT);
        assertEquals("5676", box.getStringValue("amqp"));
        assertEquals(15676, box.getIntValue("mgmt"));
        assertEquals(25676, box.getIntValue("dist"));
    }

    @Test
    void blankOrNullStringYieldsEmptySingleton() {
        assertSame(PropertyBox.EMPTY, PropertyBox.loadFromString(null));
        assertSame(PropertyBox.EMPTY, PropertyBox.loadFromString(""));
        assertSame(PropertyBox.EMPTY, PropertyBox.loadFromString("   \n "));
    }

    @Test
    void streamFactoryParsesAndNullStreamYieldsEmpty() {
        PropertyBox box = PropertyBox.loadFromStream(new ByteArrayInputStream(PORTS_TEXT.getBytes()));
        assertEquals(5676, box.getIntValue("amqp"));
        assertSame(PropertyBox.EMPTY, PropertyBox.loadFromStream(null));
    }

    @Test
    void fileFactoryLoadsRealFileAndMissingFileYieldsEmpty() throws IOException {
        Path dir = java.nio.file.Files.createTempDirectory("propertybox-test");
        File file = dir.resolve("ports.properties").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(PORTS_TEXT);
        }
        assertEquals(5676, PropertyBox.loadFromFile(file).getIntValue("amqp"));
        assertSame(PropertyBox.EMPTY, PropertyBox.loadFromFile(new File(dir.toFile(), "not-exists.properties")));
        assertSame(PropertyBox.EMPTY, PropertyBox.loadFromFile(null));
    }

    @Test
    void propertiesFactoryHoldsReferenceAndNullYieldsEmptyBox() {
        Properties properties = new Properties();
        properties.setProperty("port", "3306");
        PropertyBox box = PropertyBox.loadFromProperties(properties);
        assertEquals(3306, box.getIntValue("port"));
        //共享引用语义：外部修改即时可见。
        properties.setProperty("port", "3307");
        assertEquals(3307, box.getIntValue("port"));
        assertNotNull(PropertyBox.loadFromProperties(null).getProperties());
    }

    @Test
    void typedGettersReturnDefaultOnMissingBlankOrMalformed() {
        PropertyBox box = PropertyBox.loadFromString("port= 8080 \nbad=abc\nempty=\nenabled=TRUE\nratio=0.5");
        //前后空格容忍（trim 后解析）。
        assertEquals(8080, box.getIntValue("port"));
        //畸形值与空白值一律默认值，不抛异常。
        assertEquals(-1, box.getIntValue("bad", -1));
        assertEquals(7, box.getIntValue("empty", 7));
        assertEquals(9, box.getIntValue("missing", 9));
        assertEquals(0, box.getIntValue("missing"));
        assertEquals(99L, box.getLongValue("bad", 99L));
        assertEquals(0.5d, box.getDoubleValue("ratio"));
        assertEquals(-1.5d, box.getDoubleValue("bad", -1.5d));
        //布尔：大小写不敏感，值存在非 true 即 false；空白值与缺失走默认值。
        assertTrue(box.getBooleanValue("enabled"));
        assertFalse(box.getBooleanValue("bad", true));
        assertTrue(box.getBooleanValue("empty", true));
        assertTrue(box.getBooleanValue("missing", true));
        //字符串：空白值与缺失返回默认。
        assertEquals("fallback", box.getStringValue("empty", "fallback"));
        assertEquals("", box.getStringValue("missing"));
    }

    @Test
    void emptySingletonAlwaysDefaults() {
        assertEquals(0, PropertyBox.EMPTY.getIntValue("any"));
        assertEquals("", PropertyBox.EMPTY.getStringValue("any"));
        assertFalse(PropertyBox.EMPTY.getBooleanValue("any"));
        assertTrue(PropertyBox.EMPTY.getProperties().isEmpty());
    }

    @Test
    void getPropertiesExposesRawView() {
        PropertyBox box = PropertyBox.loadFromString(PORTS_TEXT);
        assertEquals("5676", box.getProperties().getProperty("amqp"));
        assertEquals(3, box.getProperties().size());
    }
}
