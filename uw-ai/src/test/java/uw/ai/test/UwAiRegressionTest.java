package uw.ai.test;

import uw.ai.util.BeanOutputConverter;
import uw.ai.vo.AiImageGenerateParam;
import uw.ai.vo.AiTranslateListParam;
import uw.ai.vo.AiTranslateMapParam;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 本模块回归测试（纯逻辑，无需 Spring/网络）。
 * <p>
 * 覆盖历次评审修复点：
 * <ul>
 *   <li>翻译参数 Builder 的 configCode 透传（原 Builder 缺失 configCode）</li>
 *   <li>图片参数 Builder 的 prompt → userPrompt 映射</li>
 *   <li>BeanOutputConverter.cleanJson 的 markdown / think 标签清洗</li>
 * </ul>
 * 用法：直接运行 main，全部通过输出"全部通过"，失败抛 AssertionError。
 * <p>
 * 运行方式（reactor 父 pom 未 flatten，需从 uw-base 根以 -am 构建）：
 * <pre>
 * cd backend/uw-base
 * mvn -pl uw-ai -am test-compile
 * # 组装 classpath（含 jackson/victools 等依赖）后：
 * java -cp uw-ai/target/classes:uw-ai/target/test-classes:&lt;deps&gt; uw.ai.test.UwAiRegressionTest
 * </pre>
 */
public class UwAiRegressionTest {

    private static int passed = 0;

    public static void main(String[] args) {
        testTranslateListParamConfigCode();
        testTranslateMapParamConfigCode();
        testTranslateListParamCopyBuilder();
        testImageParamPromptMapping();
        testCleanJsonPlain();
        testCleanJsonMarkdown();
        testCleanJsonThinkTag();
        System.out.println("全部通过，共 " + passed + " 项。");
    }

    /**
     * 验证 AiTranslateListParam.Builder 支持 configCode 设置与回读。
     */
    static void testTranslateListParamConfigCode() {
        AiTranslateListParam param = AiTranslateListParam.builder()
                .configId(1L)
                .configCode("default-translate")
                .textList(List.of("Hello"))
                .langList(List.of("zh-CN"))
                .build();
        assertEquals("default-translate", param.getConfigCode(), "AiTranslateListParam.configCode 未透传");
        assertEquals(1L, param.getConfigId(), "AiTranslateListParam.configId 未透传");
        // builder(copy) 也应回读 configCode
        AiTranslateListParam copy = AiTranslateListParam.builder(param).build();
        assertEquals("default-translate", copy.getConfigCode(), "AiTranslateListParam builder(copy) 未回读 configCode");
    }

    /**
     * 验证 AiTranslateMapParam.Builder 支持 configCode 设置与回读。
     */
    static void testTranslateMapParamConfigCode() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("greeting", "Hello");
        AiTranslateMapParam param = AiTranslateMapParam.builder()
                .configId(2L)
                .configCode("map-translate")
                .textMap(map)
                .langList(List.of("en"))
                .build();
        assertEquals("map-translate", param.getConfigCode(), "AiTranslateMapParam.configCode 未透传");
        assertEquals(2L, param.getConfigId(), "AiTranslateMapParam.configId 未透传");
    }

    /**
     * 验证 AiTranslateListParam 的 builder(copy) 完整复制所有字段。
     */
    static void testTranslateListParamCopyBuilder() {
        AiTranslateListParam src = AiTranslateListParam.builder()
                .configId(9L)
                .configCode("c9")
                .systemPrompt("sys")
                .langList(List.of("ja"))
                .textList(List.of("a", "b"))
                .build();
        AiTranslateListParam copy = AiTranslateListParam.builder(src).build();
        assertEquals(9L, copy.getConfigId(), "copy configId");
        assertEquals("c9", copy.getConfigCode(), "copy configCode");
        assertEquals("sys", copy.getSystemPrompt(), "copy systemPrompt");
        assertEquals(1, copy.getLangList().size(), "copy langList size");
        assertEquals(2, copy.getTextList().size(), "copy textList size");
    }

    /**
     * 验证 AiImageGenerateParam.Builder 的 prompt(...) 正确映射到 userPrompt 字段。
     */
    static void testImageParamPromptMapping() {
        AiImageGenerateParam param = AiImageGenerateParam.builder()
                .configCode("default-image")
                .prompt("一只猫")
                .sessionId(123L)
                .build();
        assertEquals("一只猫", param.getUserPrompt(), "AiImageGenerateParam.prompt 未映射到 userPrompt");
        assertEquals(123L, param.getSessionId(), "AiImageGenerateParam.sessionId 未透传");
        assertEquals("default-image", param.getConfigCode(), "AiImageGenerateParam.configCode 未透传");
    }

    /**
     * 普通 JSON 文本应原样返回（去空白）。
     */
    static void testCleanJsonPlain() {
        BeanOutputConverter<Object> converter = new BeanOutputConverter<>(Object.class);
        String out = converter.cleanJson("  {\"a\":1}  ");
        assertEquals("{\"a\":1}", out, "普通 JSON 清洗结果不符");
    }

    /**
     * ```json ... ``` 包裹的内容应被正确剥离。
     */
    static void testCleanJsonMarkdown() {
        BeanOutputConverter<Object> converter = new BeanOutputConverter<>(Object.class);
        String out = converter.cleanJson("```json\n{\"a\":1}\n```");
        assertEquals("{\"a\":1}", out, "markdown json 清洗结果不符");
    }

    /**
     * &lt;/think&gt; 之前的内容（含思考过程）应被去除。
     */
    static void testCleanJsonThinkTag() {
        BeanOutputConverter<Object> converter = new BeanOutputConverter<>(Object.class);
        String out = converter.cleanJson("<think>some reasoning</think>{\"a\":1}");
        assertEquals("{\"a\":1}", out, "think 标签清洗结果不符");
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        boolean ok = (expected == null) ? actual == null : expected.equals(actual);
        if (!ok) {
            throw new AssertionError(msg + "，期望=[" + expected + "]，实际=[" + actual + "]");
        }
        passed++;
    }

    private static void assertEquals(long expected, long actual, String msg) {
        if (expected != actual) {
            throw new AssertionError(msg + "，期望=[" + expected + "]，实际=[" + actual + "]");
        }
        passed++;
    }

    private static void assertEquals(int expected, int actual, String msg) {
        if (expected != actual) {
            throw new AssertionError(msg + "，期望=[" + expected + "]，实际=[" + actual + "]");
        }
        passed++;
    }
}
