package uw.common.vo;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * {@link JsonConfigParam} 接口的 Jackson 序列化与 i18n 行为验证。
 * <p>
 * 重点防御：
 * <ul>
 *   <li>序列化输出字段名为 {@code title}（来自 {@code getLocalizedTitle()} 的 {@code @JsonProperty}），不泄漏 {@code getTitle()} 原始值。</li>
 *   <li>{@code key} 由枚举名自动派生。</li>
 *   <li>未配置 i18n 时 {@code getLocalizedTitle()} 回退到 {@code getTitle()} 原始值。</li>
 * </ul>
 */
public class JsonConfigParamSerializeTest {

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    /**
     * 验证序列化输出字段名与 i18n 双方法模式。
     */
    @Test
    void testSerializationFieldNameAndNoRawLeak() throws Exception {
        JsonConfigParam param = SampleConfig.TEMPERATURE;
        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode node = mapper.valueToTree(param);

        // 期望字段：key / type / value / title / regex
        Assertions.assertTrue(node.has("key"), "应有 key 字段");
        Assertions.assertTrue(node.has("type"), "应有 type 字段");
        Assertions.assertTrue(node.has("value"), "应有 value 字段");
        Assertions.assertTrue(node.has("title"), "title 应作为 JSON 字段输出（getLocalizedTitle 的 @JsonProperty）");
        Assertions.assertTrue(node.has("regex"), "应有 regex 字段");

        // 绝不应出现原始值出口或内部方法作为字段
        Assertions.assertFalse(node.has("localizedTitle"), "getLocalizedTitle 不应作为字段输出");
        Assertions.assertFalse(node.has("rawTitle"), "getTitle 不应作为字段输出");
        Assertions.assertFalse(node.has("paramData"), "getParamData 不应作为字段输出（@JsonIgnore）");

        // 严格字段总数：恰好 5 个（key/type/value/title/regex），防止 default 委托链引入意外字段
        int size = node.size();
        Assertions.assertEquals(5, size, "字段总数应为 5（key/type/value/title/regex），实际: " + size + " -> " + node.toPrettyString());

        // key 由枚举名派生：TEMPERATURE → temperature
        Assertions.assertEquals("temperature", node.get("key").asString(),
                "key 应由枚举名自动派生");

        // 未配置 i18n 时，title 字段值 = getTitle() 原始值（回退）
        Assertions.assertEquals("采样温度", node.get("title").asString(),
                "未配置 i18n 时 title 度回退到 getTitle() 原始值");

        // getTitle() 返回原始值（不被 getLocalizedTitle 干扰）
        Assertions.assertEquals("采样温度", param.getTitle(), "getTitle() 应返回原始值");
        Assertions.assertEquals("采样温度", param.getLocalizedTitle(),
                "无 i18n 资源时 getLocalizedTitle() 回退原始值");
    }

    /**
     * 验证 getValue / getType / getRegex 正常输出。
     */
    @Test
    void testBasicFields() throws Exception {
        JsonConfigParam param = SampleConfig.MAX_TOKENS;
        JsonNode node = JsonMapper.builder().build().valueToTree(param);

        Assertions.assertEquals("max.tokens", node.get("key").asString());
        Assertions.assertEquals("int", node.get("type").asString());
        Assertions.assertEquals("4096", node.get("value").asString());
        Assertions.assertEquals("采样温度", node.get("title").asString(), "title 应回退原始值");
    }

    /**
     * 测试用枚举实现。
     */
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT)
    enum SampleConfig implements JsonConfigParam {
        TEMPERATURE(JsonConfigParam.ParamType.FLOAT, "0.8", "采样温度", null),
        MAX_TOKENS(JsonConfigParam.ParamType.INT, "4096", "采样温度", null),
        ;

        private final JsonConfigParam.ParamData paramData;

        SampleConfig(JsonConfigParam.ParamType type, String value, String title, String regex) {
            this.paramData = new JsonConfigParam.ParamData(type, value, title, regex);
        }

        @Override
        public JsonConfigParam.ParamData getParamData() {
            return paramData;
        }
    }
}
