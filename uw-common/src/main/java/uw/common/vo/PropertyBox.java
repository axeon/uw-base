package uw.common.vo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serial;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

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
     * 空的配置参数盒子（工厂失败与空输入的安全默认值），所有取值恒返默认值。
     * <p>
     * 共享单例，结构性只读：内部载体对全部变更入口（put/setProperty/load/remove/clear/compute 族及视图删除）
     * 抛 {@link UnsupportedOperationException}——共享实例一旦被某调用方原地写入，变量会串入全部空输入的
     * 读取方，故违规写入在写入点立即失败，而不是静默扩散。需要可写变量表的调用方先取私有副本：
     * {@code new Properties()} 后 {@code putAll(box.getProperties())}，或
     * {@code (Properties) box.getProperties().clone()}（EMPTY 的 clone 返回普通可写 Properties）。
     */
    public static final PropertyBox EMPTY = new PropertyBox(new ReadOnlyProperties());

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
     * 流为 null 或读取失败降级为 {@link #EMPTY}（null 属常态输入不告警，IO 失败/文本损坏记 WARN）。
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
        } catch (IOException | IllegalArgumentException e) {
            //文本损坏（畸形 unicode 转义）由 Properties.load 抛 IllegalArgumentException 而非 IOException，须一并降级。
            log.warn("PropertyBox load from stream failed: {}", e.toString());
            return EMPTY;
        }
        return new PropertyBox(properties);
    }

    /**
     * 从字符串加载（Properties 文本格式，行分隔的 key=value）。
     * 空白输入（含 null，DB 空列属常态）返回 {@link #EMPTY} 不告警；文本损坏（如畸形 unicode 转义）
     * 记 WARN 并返回 {@link #EMPTY}。
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
        } catch (IOException | IllegalArgumentException e) {
            //文本损坏（畸形 unicode 转义）由 Properties.load 抛 IllegalArgumentException 而非 IOException，须一并降级。
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
     * 获取持有的原始 Properties（共享引用——键值遍历/整包复用场景；修改即修改盒子本身）。
     * <p>
     * {@link #EMPTY} 返回只读实例（变更抛 {@link UnsupportedOperationException}）；工厂对空白/失败输入
     * 返回的正是 EMPTY，故需要写入时一律先取私有副本，勿在返回值上原地修改。
     *
     * @return 持有的 Properties
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

    /**
     * 只读 Properties：{@link #EMPTY} 的内部载体，恒为空。
     * <p>
     * 封堵面=JDK Properties 全部公开变更入口（Properties 已覆写 Hashtable 全部公开方法并委托内部 Map，
     * 故逐一覆写即完整）：写入族（put/putAll/putIfAbsent/setProperty/load/loadFromXML/replace/replaceAll/
     * compute 族/merge）与删除族（remove/clear）抛 {@link UnsupportedOperationException}；keySet/values/entrySet
     * 包为不可修改视图。{@link #clone()} 返回普通可写 Properties——副本即调用方私有，应当可写，
     * 不能让"克隆出只读对象"把私有副本惯用法也拦住。
     */
    private static final class ReadOnlyProperties extends Properties {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 构造只读变更异常（固定文案，指明正确用法）。
         *
         * @return 异常
         */
        private static UnsupportedOperationException readOnly() {
            return new UnsupportedOperationException("PropertyBox.EMPTY is shared and read-only, copy it before modifying.");
        }

        @Override
        public Object setProperty(String key, String value) {
            throw readOnly();
        }

        @Override
        public void load(Reader reader) {
            throw readOnly();
        }

        @Override
        public void load(InputStream inStream) {
            throw readOnly();
        }

        @Override
        public void loadFromXML(InputStream in) {
            throw readOnly();
        }

        @Override
        public Object put(Object key, Object value) {
            throw readOnly();
        }

        @Override
        public void putAll(Map<?, ?> t) {
            throw readOnly();
        }

        @Override
        public Object putIfAbsent(Object key, Object value) {
            throw readOnly();
        }

        @Override
        public Object remove(Object key) {
            throw readOnly();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw readOnly();
        }

        @Override
        public void clear() {
            throw readOnly();
        }

        @Override
        public Object replace(Object key, Object value) {
            throw readOnly();
        }

        @Override
        public boolean replace(Object key, Object oldValue, Object newValue) {
            throw readOnly();
        }

        @Override
        public void replaceAll(BiFunction<? super Object, ? super Object, ?> function) {
            throw readOnly();
        }

        @Override
        public Object computeIfAbsent(Object key, Function<? super Object, ?> mappingFunction) {
            throw readOnly();
        }

        @Override
        public Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
            throw readOnly();
        }

        @Override
        public Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
            throw readOnly();
        }

        @Override
        public Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
            throw readOnly();
        }

        @Override
        public Set<Object> keySet() {
            return Collections.unmodifiableSet(super.keySet());
        }

        @Override
        public Collection<Object> values() {
            return Collections.unmodifiableCollection(super.values());
        }

        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
            return Collections.unmodifiableSet(super.entrySet());
        }

        @Override
        public Object clone() {
            Properties copy = new Properties();
            copy.putAll(this);
            return copy;
        }
    }

}
