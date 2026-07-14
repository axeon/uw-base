package uw.common.helper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import uw.common.constant.CommonConstants;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将对象属性展开为URI查询参数的工具类。
 * 支持PageQueryParam/QueryParam的$魔法参数自动映射，
 * 过滤Auth系参数(saasId/userId/mchId/userType)，
 * 使用Caffeine缓存反射元数据提升性能。
 */
public class QueryParamHelper {

    /**
     * 缓存
     */
    private static final Cache<Class<?>, List<FieldMeta>> CLS_META_CACHE = Caffeine.newBuilder()
            .maximumSize(10000)
            .build();

    /**
     * 忽略参数, 此参数后台设定，不需要传参。
     */
    private static final Set<String> IGNORE_PARAMS = Set.of("saasId", "userId", "mchId", "userType", "LIKE_QUERY_PARAM_MIN_LEN", "LIKE_QUERY_ENABLE", "SELECT_SQL", "EXT_COND_SQL", "EXT_COND_MAP");

    /**
     * PageQueryParam/QueryParam的魔法参数：字段名 → URL参数名
     */
    private static final Map<String, String> MAGIC_PARAMS = Map.of(
            "PAGE", "$pg",
            "RESULT_NUM", "$rn",
            "START_INDEX", "$si",
            "REQUEST_TYPE", "$rt",
            "SORT_NAME", "$sn",
            "SORT_TYPE", "$st"
    );

    /**
     * 将对象属性展开为 URI 查询参数。
     * <p>
     * 遍历 queryParam（含父类）的非 static/非 transient 业务字段，将非空值附加到 baseUrl 之后。
     * 过滤 Auth 系字段（saasId/userId/mchId/userType）与魔法字段，魔法参数按 {@link #MAGIC_PARAMS} 映射为短名（如 PAGE→$pg）。
     * 参数值按 UTF-8 进行 URL 编码（application/x-www-form-urlencoded），多个值生成同名多值参数。
     * </p>
     *
     * @param baseUrl    基础 URL
     * @param queryParam 查询参数对象，为 null 时原样返回 baseUrl
     * @return 附加查询参数后的 URL 字符串
     */
    public static String buildUriWithParams(String baseUrl, Object queryParam) {
        // 参数为空则返回
        if (queryParam == null) {
            return baseUrl;
        }
        // 获取反射元数据
        List<FieldMeta> metaList = CLS_META_CACHE.get(queryParam.getClass(), QueryParamHelper::buildFieldMetas);
        // 缓存为空则返回
        if (metaList.isEmpty()) {
            return baseUrl;
        }
        StringBuilder url = new StringBuilder(baseUrl);
        // 首个追加的参数用 '?'，之后用 '&'；baseUrl 已含查询串时直接用 '&'
        boolean useQuestionMark = baseUrl.indexOf('?') < 0;

        // 处理普通业务字段
        for (FieldMeta meta : metaList) {
            try {
                Object value = meta.field.get(queryParam);
                if (value == null) {
                    continue;
                }
                useQuestionMark = appendValue(url, meta.paramName, value, useQuestionMark);
            } catch (Exception ignored) {
            }
        }
        return url.toString();
    }

    /**
     * 将单个属性值附加为 URI 查询参数。
     * <p>
     * 数组（含基本类型数组）与 Iterable 展开为同名多值参数；空数组/空集合不附加；
     * 普通值在转为空字符串时不附加。所有值按 UTF-8 进行 URL 编码。
     * </p>
     *
     * @param url           正在构建的 URL 字符串
     * @param paramName     参数名
     * @param value         参数值（非 null）
     * @param useQuestionMark 当前是否应使用 '?' 作为分隔符（首个追加参数且 baseUrl 无查询串时为 true）
     * @return 后续追加应使用的分隔符状态（一旦追加过参数即为 false）
     */
    private static boolean appendValue(StringBuilder url, String paramName, Object value, boolean useQuestionMark) {
        if (value.getClass().isArray()) {
            // 兼容基本类型数组（int[]/long[]/...），避免直接强转 Object[] 抛 ClassCastException 导致参数被静默丢弃。
            int length = Array.getLength(value);
            if (length == 0) {
                return useQuestionMark;
            }
            for (int i = 0; i < length; i++) {
                useQuestionMark = appendSingle(url, paramName, String.valueOf(Array.get(value, i)), useQuestionMark);
            }
            return useQuestionMark;
        } else if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                useQuestionMark = appendSingle(url, paramName, String.valueOf(item), useQuestionMark);
            }
            return useQuestionMark;
        } else {
            String strValue = String.valueOf(value);
            if (strValue.isEmpty()) {
                return useQuestionMark;
            }
            return appendSingle(url, paramName, strValue, useQuestionMark);
        }
    }

    /**
     * 追加单个键值对到 URL 查询串。
     * <p>
     * 按 UTF-8 对参数名与值进行 URL 编码（{@link URLEncoder#encode(String, String)}），
     * 输出形如 {@code name=value}，并以 '?'（首个参数）或 '&'（后续参数）作为分隔符。
     * </p>
     *
     * @param url           正在构建的 URL 字符串
     * @param paramName     参数名
     * @param strValue      参数值字符串
     * @param useQuestionMark 是否使用 '?' 作为分隔符
     * @return 固定返回 false（追加后后续一律使用 '&'）
     */
    private static boolean appendSingle(StringBuilder url, String paramName, String strValue, boolean useQuestionMark) {
        url.append(useQuestionMark ? '?' : '&');
        url.append(encode(paramName)).append('=').append(encode(strValue));
        return false;
    }

    /**
     * 按 UTF-8 编码 URL 查询参数值。
     *
     * @param value 原始值
     * @return URL 编码后的值
     */
    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, CommonConstants.UTF_8);
        } catch (UnsupportedEncodingException e) {
            // UTF-8 是所有 JDK 必然支持的编码，理论上不会到达此分支
            return value;
        }
    }

    /**
     * 构建类的反射元数据缓存。
     * 只收集有标准getter(setter对)的业务字段，跳过static/Auth/魔法字段。
     */
    private static List<FieldMeta> buildFieldMetas(Class<?> clazz) {
        List<FieldMeta> metas = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                    continue;
                }
                field.setAccessible(true);
                String fieldName = field.getName();
                // 跳过Auth系字段
                if (IGNORE_PARAMS.contains(fieldName)) {
                    continue;
                }
                String magicParam = MAGIC_PARAMS.get(fieldName);
                if (magicParam != null) {
                    metas.add(new FieldMeta(magicParam, field));
                    continue;
                } else {
                    metas.add(new FieldMeta(fieldName, field));
                }
            }
            current = current.getSuperclass();
        }
        return metas;
    }

    /**
     * 类字段元数据。
     *
     * @param paramName URL 参数名（魔法参数映射后的短名或原字段名）
     * @param field     反射字段
     */
    private record FieldMeta(String paramName, Field field) {
    }

}
