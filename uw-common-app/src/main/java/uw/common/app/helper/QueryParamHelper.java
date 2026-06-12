package uw.common.app.helper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.web.util.UriComponentsBuilder;
import uw.common.dto.QueryParam;

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
     * 将对象属性展开为URI查询参数。
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
     * 将对象属性展开为URI查询参数。
     */
    private static void appendValue(UriComponentsBuilder builder, String paramName, Object value) {
        if (value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            if (arr.length == 0) {
                return;
            }
            for (Object item : arr) {
                builder.queryParam(paramName, String.valueOf(item));
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
     */
    private record FieldMeta(String paramName, Field field) {
    }

}
