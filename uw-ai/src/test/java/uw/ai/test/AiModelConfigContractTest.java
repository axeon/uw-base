package uw.ai.test;

import tools.jackson.databind.JsonNode;
import uw.ai.vo.AiModelConfigVo;
import uw.common.util.JsonUtils;

/**
 * 无需网络的 SDK 缓存计费 JSON 契约回归。
 */
public class AiModelConfigContractTest {

    /**
     * 验证缓存计费参数与参考价的整数序列化及反序列化。
     *
     * @param args 命令行参数，本测试不使用
     */
    public static void main(String[] args) {
        for (int value : new int[]{0, 250, Integer.MAX_VALUE}) {
            AiModelConfigVo config = new AiModelConfigVo();
            config.setCreditCacheWrite(value);
            config.setPriceCacheWrite(value);
            String json = JsonUtils.toString(config);
            JsonNode tree = JsonUtils.getJsonMapper().readTree(json);
            if (!tree.get("creditCacheWrite").isIntegralNumber() || !tree.get("priceCacheWrite").isIntegralNumber()) {
                throw new AssertionError("Cache write rates must serialize as integer numbers");
            }
            AiModelConfigVo copy = JsonUtils.parse(json, AiModelConfigVo.class);
            if (config.getCreditCacheWrite() != copy.getCreditCacheWrite()
                    || config.getPriceCacheWrite() != copy.getPriceCacheWrite()
                    || tree.has("creditCached") || tree.has("priceCached")) {
                throw new AssertionError("Cache read/write contract changed values or exposes old names");
            }
        }
        System.out.println("SDK cache billing contract: 3 integer round trips passed");
    }
}
