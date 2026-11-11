package uw.common.vo;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PropertyBox 单测：四源工厂构造（字符串/流/文件/Properties）、空输入与失败降级 EMPTY、
 * 强类型取值族的默认值语义（缺失/空白/畸形一律默认值不抛异常）、getProperties 原始暴露、
 * 共享 EMPTY 结构性只读（全部变更入口被拒、降级返回值不可污染后续调用、私有副本可写）。
 */
class PropertyBoxTest {

    /**
     * 标准端口映射样例（ops 系 DB 列的典型形态）。
     */
    private static final String PORTS_TEXT = "amqp=5676\nmgmt=15676\ndist=25676\n";

    /**
     * 损坏文本样例：首行合法、次行畸形 unicode 转义（Properties.load 抛 IllegalArgumentException）。
     */
    private static final String CORRUPT_TEXT = "amqp=5676\nmgmt=\\uZZZZ\n";

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
        Path dir = Files.createTempDirectory("propertybox-test");
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

    @Test
    void emptySingletonRejectsEveryMutation() {
        Properties shared = PropertyBox.EMPTY.getProperties();
        assertAll(
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.setProperty("APP_NAME", "polluter")),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.put("APP_NAME", "polluter")),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.putAll(Map.of("APP_NAME", "polluter"))),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.putIfAbsent("APP_NAME", "polluter")),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.load(new StringReader(""))),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.load(new ByteArrayInputStream(PORTS_TEXT.getBytes()))),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.loadFromXML(new ByteArrayInputStream(new byte[0]))),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.remove("APP_NAME")),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.remove("APP_NAME", "polluter")),
                () -> assertThrows(UnsupportedOperationException.class, shared::clear),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.replace("APP_NAME", "polluter")),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.replace("APP_NAME", "old", "polluter")),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.replaceAll((key, value) -> value)),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.computeIfAbsent("APP_NAME", key -> "polluter")),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.computeIfPresent("APP_NAME", (key, value) -> "polluter")),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.compute("APP_NAME", (key, value) -> "polluter")),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.merge("APP_NAME", "polluter", (oldValue, value) -> value)),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.keySet().add("APP_NAME")),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.keySet().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.values().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.entrySet().add(Map.entry("APP_NAME", "polluter"))),
                () -> assertThrows(UnsupportedOperationException.class, () -> shared.entrySet().clear())
        );
        //全部写入被拒后共享实例保持空，取值族语义不变。
        assertTrue(shared.isEmpty());
        assertEquals("", PropertyBox.EMPTY.getStringValue("APP_NAME"));
    }

    @Test
    void degradedLoadsCannotPolluteLaterCalls() {
        //null/空白/损坏文本一律降级为共享 EMPTY，对返回值的原地写入（如向空环境变量表追加 APP_NAME）被拒。
        for (String input : new String[]{null, "", "   \n ", CORRUPT_TEXT}) {
            PropertyBox degraded = PropertyBox.loadFromString(input);
            assertSame(PropertyBox.EMPTY, degraded);
            assertThrows(UnsupportedOperationException.class, () -> degraded.getProperties().setProperty("APP_NAME", "polluter"));
        }
        //后续空输入读取方拿到的仍是干净空表：不会把他方变量当端口表解析，也不会被注入他方变量。
        PropertyBox later = PropertyBox.loadFromString(null);
        assertTrue(later.getProperties().isEmpty());
        assertTrue(later.getProperties().stringPropertyNames().isEmpty());
        assertEquals("", later.getStringValue("APP_NAME"));
        //损坏文本已解析出的合法前缀行同样随降级丢弃，不外泄。
        assertEquals(0, PropertyBox.loadFromString(CORRUPT_TEXT).getIntValue("amqp"));
    }

    @Test
    void corruptTextDegradesToEmptyInsteadOfThrowing() throws IOException {
        assertSame(PropertyBox.EMPTY, PropertyBox.loadFromString(CORRUPT_TEXT));
        assertSame(PropertyBox.EMPTY, PropertyBox.loadFromStream(new ByteArrayInputStream(CORRUPT_TEXT.getBytes())));
        Path dir = Files.createTempDirectory("propertybox-corrupt");
        File file = dir.resolve("corrupt.properties").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(CORRUPT_TEXT);
        }
        assertSame(PropertyBox.EMPTY, PropertyBox.loadFromFile(file));
    }

    @Test
    void privateCopyOfEmptyIsWritableAndIsolated() {
        //两种私有副本惯用法均得到普通可写 Properties，写入不回流共享 EMPTY。
        Properties cloned = (Properties) PropertyBox.EMPTY.getProperties().clone();
        cloned.setProperty("APP_NAME", "mine");
        Properties copied = new Properties();
        copied.putAll(PropertyBox.EMPTY.getProperties());
        copied.setProperty("APP_NAME", "mine");
        assertEquals("mine", cloned.getProperty("APP_NAME"));
        assertEquals("mine", copied.getProperty("APP_NAME"));
        assertTrue(PropertyBox.EMPTY.getProperties().isEmpty());
    }

    @Test
    void parsedBoxesStayWritableAndIndependent() {
        //非 EMPTY 实例语义不变：getProperties 为共享引用，修改即修改盒子本身；每次解析各自独立。
        PropertyBox first = PropertyBox.loadFromString(PORTS_TEXT);
        PropertyBox second = PropertyBox.loadFromString(PORTS_TEXT);
        first.getProperties().setProperty("amqp", "5677");
        assertEquals(5677, first.getIntValue("amqp"));
        assertEquals(5676, second.getIntValue("amqp"));
    }
}
