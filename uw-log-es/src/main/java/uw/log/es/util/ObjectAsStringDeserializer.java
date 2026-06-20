package uw.log.es.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 将 Json 节点反序列化为字符串而非对象。
 * <p>与 {@link ObjectAsStringSerializer} 配对：把字段值（原为 JSON 字符串）解析回节点后，
 * 再次序列化为规范化 JSON 字符串，保持写入/读取往返一致。
 */
public class ObjectAsStringDeserializer extends JsonDeserializer<String> {

    /**
     * 将当前 Json 节点读取后重新序列化为字符串。
     *
     * @param jp   Json 解析器
     * @param ctxt 反序列化上下文
     * @return 规范化的 JSON 字符串
     * @throws IOException 读取异常
     */
    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = mapper.readTree(jp);
        return mapper.writeValueAsString(node);
    }
}
