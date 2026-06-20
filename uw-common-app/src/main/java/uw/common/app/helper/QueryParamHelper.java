package uw.common.app.helper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
            .maximumSize(5000)
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
     * </p>
     *
     * @param baseUrl   基础 URL
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
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);

        // 处理普通业务字段
        for (FieldMeta meta : metaList) {
            try {
                Object value = meta.field.get(queryParam);
                if (value == null) {
                    continue;
                }
                appendValue(builder, meta.paramName, value);
            } catch (Exception ignored) {
            }
        }
        return builder.toUriString();
    }

    /**
     * 将单个属性值附加为 URI 查询参数。
     * <p>
     * 数组（含基本类型数组）与 Iterable 展开为同名多值参数；空数组/空集合不附加；
     * 普通值在转为空字符串时不附加。
     * </p>
     *
     * @param builder   URI 构建器
     * @param paramName 参数名
     * @param value     参数值（非 null）
     */
    private static void appendValue(UriComponentsBuilder builder, String paramName, Object value) {
        if (value.getClass().isArray()) {
            // 兼容基本类型数组（int[]/long[]/...），避免直接强转 Object[] 抛 ClassCastException 导致参数被静默丢弃。
            int length = Array.getLength(value);
            if (length == 0) {
                return;
            }
            for (int i = 0; i < length; i++) {
                builder.queryParam(paramName, String.valueOf(Array.get(value, i)));
            }
        } else if (value instanceof Iterable<?> iterable) {
            boolean added = false;
            for (Object item : iterable) {
                builder.queryParam(paramName, String.valueOf(item));
                added = true;
            }
            if (!added) {
                return;
            }
        } else {
            String strValue = String.valueOf(value);
            if (strValue.isEmpty()) {
                return;
            }
            builder.queryParam(paramName, value);
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
