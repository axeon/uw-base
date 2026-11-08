package uw.common.vo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Properties;

/**
 * Properties 配置参数盒子。
 * <p>
 * 持有一份 {@link Properties}，提供强类型读取方法（字符串/整数/长整数/浮点/布尔，均带默认值重载）
 * 与多源工厂构造（文件/流/字符串/已有 Properties 对象）。工厂解析失败（IO 错误/文本损坏）降级为
 * {@link #EMPTY} 并记 WARN，不抛异常——配置读取不得阻断启动或调用方主流程；读取失败返回默认值。
 * 语义与 {@link JsonConfigBox}（Map 载体版）同族：载体不同（Properties 的等号分隔文本 vs JSON 合并 Map），
 * 各自独立演进。
 * <p>
 * 典型场景：DB 列以 Properties 文本格式存储的配置解析（如端口映射 "amqp=5676\nmgmt=15676"、
 * profile 环境变量）、classpath 资源文件、磁盘配置文件。
 */
public class PropertyBox {

    /**
     * 空的配置参数盒子（内部 Properties 为空实例，作为工厂失败与空输入的安全默认值）。
     * <p>
     * 共享单例：所有取值恒返默认值；{@link #getProperties()} 返回的空 Properties 不可修改
     * （修改会污染全部 EMPTY 引用方）。
     */
    public static final PropertyBox EMPTY = new PropertyBox(new Properties());

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(PropertyBox.class);

    /**
     * 持有的配置。
     */
    private final Properties properties;

    /**
     * 构造指定 Properties 的盒子。
     *
     * @param properties 配置（持有引用，调用方勿再修改）
     */
    public PropertyBox(Properties properties) {
        this.properties = properties == null ? new Properties() : properties;
    }

    /**
     * 从磁盘文件加载（Properties 文本格式）。
     * 文件不存在或读取失败降级为 {@link #EMPTY} 并记 WARN，不抛异常。
     *
     * @param file 配置文件
     * @return 配置盒子
     */
    public static PropertyBox loadFromFile(File file) {
        if (file == null || !file.isFile()) {
            log.warn("PropertyBox load from file skipped: file not found [{}]", file);
            return EMPTY;
        }
        try (InputStream in = new FileInputStream(file)) {
            return loadFromStream(in);
        } catch (IOException e) {
            log.warn("PropertyBox load from file failed [{}]: {}", file, e.toString());
            return EMPTY;
        }
    }

    /**
     * 从输入流加载（Properties 文本格式，流由调用方关闭）。
     * 流为 null 或读取失败降级为 {@link #EMPTY}（null 属常态输入不告警，IO 失败记 WARN）。
     *
     * @param in 配置输入流（classpath 资源、socket、文件流等）
     * @return 配置盒子
     */
    public static PropertyBox loadFromStream(InputStream in) {
        if (in == null) {
            return EMPTY;
        }
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            log.warn("PropertyBox load from stream failed: {}", e.toString());
            return EMPTY;
        }
        return new PropertyBox(properties);
    }

    /**
     * 从字符串加载（Properties 文本格式，行分隔的 key=value）。
     * 空白输入（含 null，DB 空列属常态）返回 {@link #EMPTY} 不告警；文本损坏记 WARN 并返回 {@link #EMPTY}。
     *
     * @param text Properties 文本（如 "amqp=5676\nmgmt=15676"）
     * @return 配置盒子
     */
    public static PropertyBox loadFromString(String text) {
        if (text == null || text.isBlank()) {
            return EMPTY;
        }
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(text));
        } catch (IOException e) {
            log.warn("PropertyBox load from string failed: {}", e.toString());
            return EMPTY;
        }
        return new PropertyBox(properties);
    }

    /**
     * 从已有 Properties 对象构造（复用调用方已加载的配置）。
     *
     * @param properties 已有配置
     * @return 配置盒子
     */
    public static PropertyBox loadFromProperties(Properties properties) {
        return new PropertyBox(properties);
    }

    /**
     * @return 持有的原始 Properties（共享引用——键值遍历/整包复用场景；修改即修改盒子本身）
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * 获取字符串值。
     *
     * @param key 参数名
     * @return 值；不存在或空白返回空串
     */
    public String getStringValue(String key) {
        return getStringValue(key, "");
    }

    /**
     * 获取字符串值。
     *
     * @param key          参数名
     * @param defaultValue 缺省值（键不存在或值空白时返回）
     * @return 值或默认值
     */
    public String getStringValue(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 获取整数值。
     *
     * @param key 参数名
     * @return 值；不存在/空白/畸形返回 0
     */
    public int getIntValue(String key) {
        return getIntValue(key, 0);
    }

    /**
     * 获取整数值。
     *
     * @param key          参数名
     * @param defaultValue 缺省值（键不存在/值空白/解析失败时返回）
     * @return 值或默认值
     */
    public int getIntValue(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null && !value.isBlank()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
                //畸形值按默认值处理（与 JsonConfigBox 语义一致，不打日志刷屏）
            }
        }
        return defaultValue;
    }

    /**
     * 获取长整数值。
     *
     * @param key 参数名
     * @return 值；不存在/空白/畸形返回 0
     */
    public long getLongValue(String key) {
        return getLongValue(key, 0L);
    }

    /**
     * 获取长整数值。
     *
     * @param key          参数名
     * @param defaultValue 缺省值（键不存在/值空白/解析失败时返回）
     * @return 值或默认值
     */
    public long getLongValue(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value != null && !value.isBlank()) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 获取浮点值。
     *
     * @param key 参数名
     * @return 值；不存在/空白/畸形返回 0
     */
    public double getDoubleValue(String key) {
        return getDoubleValue(key, 0d);
    }

    /**
     * 获取浮点值。
     *
     * @param key          参数名
     * @param defaultValue 缺省值（键不存在/值空白/解析失败时返回）
     * @return 值或默认值
     */
    public double getDoubleValue(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value != null && !value.isBlank()) {
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 获取布尔值（"true" 不区分大小写，其余均为 false）。
     *
     * @param key 参数名
     * @return 值；不存在/空白返回 false
     */
    public boolean getBooleanValue(String key) {
        return getBooleanValue(key, false);
    }

    /**
     * 获取布尔值（"true" 不区分大小写）。
     *
     * @param key          参数名
     * @param defaultValue 缺省值（键不存在或值空白时返回；值存在时按 parseBoolean 语义）
     * @return 值或默认值
     */
    public boolean getBooleanValue(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null && !value.isBlank()) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

}
