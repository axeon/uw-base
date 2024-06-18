package uw.mfa.captcha.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述: json操作工具类
 *
 */
public class CaptchaJsonUtils {

    private static final Logger logger = LoggerFactory.getLogger( CaptchaJsonUtils.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * String -> 对象
     * @param content
     * @param valueType
     * @return
     * @param <T>
     */
    public static <T> T parseObject(String content, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(content, valueType);
        } catch (JsonProcessingException e) {
            logger.error( "JsonUtils parseObject error params:{}, message: {}", content, e.getMessage(), e );
            throw new IllegalArgumentException("JsonUtil parseObject error: " + e.getMessage());
        }
    }


    /**
     * 对象 -> String
     * @param value
     * @return
     */
    public static String toJSONString(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            logger.error( "JsonUtils toJSONString error params:{}, message: {}", value, e.getMessage(), e );
            return "";
        }
    }


    /**
     *
     * @param content
     * @param clazz
     * @return
     * @param <T>
     */
    public static <T> T[] parseArray(String content, Class<? extends T[]> clazz) {
        try {
            return OBJECT_MAPPER.readValue(content, clazz);
        } catch (JsonProcessingException e) {
            logger.error( "JsonUtils parseObject error params:{}, message: {}", content, e.getMessage(), e );
            throw new IllegalArgumentException("JsonUtil parseObject error: " + e.getMessage());
        }
    }
}
