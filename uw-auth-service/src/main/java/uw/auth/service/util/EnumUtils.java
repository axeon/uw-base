package uw.auth.service.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Enum的工具类。
 */
public class EnumUtils {

    /**
     * enum数据缓存。
     */
    private static final LoadingCache<String, Map<String, Object>> enumCache = Caffeine.newBuilder().build( basePackage -> {
        Map<String, Object> enumMap = new HashMap<>();
        Reflections reflections = new Reflections(basePackage);
        Set<Class<? extends Enum>> enumCls = reflections.getSubTypesOf(Enum.class);
        enumCls.forEach(m -> {
            enumMap.put(m.getSimpleName(), m.getEnumConstants());
        });
        return enumMap;
    });

    /**
     * 根据基础包名，获得此包下所有的Enum数据Map。
     * @param basePackage
     * @return
     */
    public static Map<String, Object> getEnumMap(String basePackage){
        return enumCache.get(basePackage);
    }
}
