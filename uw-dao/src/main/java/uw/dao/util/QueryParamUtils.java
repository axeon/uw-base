package uw.dao.util;


import org.apache.commons.lang3.StringUtils;
import uw.dao.QueryParam;
import uw.dao.annotation.QueryMeta;
import uw.dao.vo.QueryMetaInfo;
import uw.dao.vo.QueryParamResult;
import uw.dao.vo.TableMetaInfo;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查询参数工具类。
 */
public class QueryParamUtils {

    /**
     * 查询信息反射缓存.
     * key：param类名。
     * value：字段数组。
     */
    private static final Map<String, List<QueryMetaInfo>> queryMetaCache = new ConcurrentHashMap<>(1280);

    /**
     * 解析queryParam。
     * 一般用于update和delete的where条件构造。
     *
     * @param queryParam 查询参数。
     * @return 转换结果
     */
    public static QueryParamResult parseQueryParam(QueryParam<?> queryParam) {
        return parseQueryParam(null, null, queryParam);
    }

    /**
     * 解析queryParam。
     *
     * @param cls        entityBean
     * @param tableName  附加表名，在分表情况下。
     * @param queryParam 查询参数。
     * @return 转换结果
     */
    public static QueryParamResult parseQueryParam(Class<?> cls, String tableName, QueryParam<?> queryParam) {
        StringBuilder sqlBuilder = new StringBuilder(256);
        ArrayList<Object> paramValueList = new ArrayList<>();
        try {
            //先判定是否指定SELECT_SQL.
            if (StringUtils.isBlank(queryParam.SELECT_SQL())) {
                if (cls != null) {
                    //拼出查询sql前半部分
                    TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(cls);
                    if (StringUtils.isBlank(emi.getSql())) {
                        sqlBuilder.append("select * from ");
                        if (StringUtils.isBlank(tableName)) {
                            sqlBuilder.append(emi.getTableName());
                        } else {
                            sqlBuilder.append(tableName);
                        }
                    } else {
                        sqlBuilder.append(emi.getSql());
                    }
                }
            } else {
                sqlBuilder.append(queryParam.SELECT_SQL());
            }

            //参数map。key=sql, value=数值
            Map<String, Object> paramMap = new LinkedHashMap<>();

            //开始拼出数值。
            sqlBuilder.append(" where 1=1");
            List<QueryMetaInfo> queryMetaInfoList = loadQueryParamMetaInfo(queryParam.getClass());
            //先收集注解参数
            for (QueryMetaInfo metaInfo : queryMetaInfoList) {
                Object ov = metaInfo.getField().get(queryParam);
                if (ov != null) {
                    paramMap.put("and " + metaInfo.getQueryExpr(), ov);
                }
            }
            //合并附加参数
            Map<String, Object> extParamMap = queryParam.EXT_COND_MAP();
            if (extParamMap != null) {
                for (Map.Entry<String, Object> kv : extParamMap.entrySet()) {
                    paramMap.put("and " + kv.getKey(), kv.getValue());
                }
            }
            //开始生成sql.
            for (Map.Entry<String, Object> kv : paramMap.entrySet()) {
                String paramCond = kv.getKey();
                Object paramValue = kv.getValue();
                if (paramValue != null) {
                    //占位符计数。
                    int placeholdersCount = StringUtils.countMatches(paramCond, "?");
                    //没有占位符的，不用处理参数。
                    if (placeholdersCount > 0) {
                        //参数数量初始为1
                        int paramValueSize = 1;
                        //需要单独处理多参数的数组对象。
                        if (paramValue.getClass().isArray()) {
                            paramValueSize = Array.getLength(paramValue);
                            for (int i = 0; i < paramValueSize; i++) {
                                paramValueList.add(Array.get(paramValue, i));
                            }
                        } else if (paramValue instanceof List<?> list) {
                            paramValueSize = list.size();
                            paramValueList.addAll(list);
                        } else {
                            paramValueList.add(paramValue);
                            //此处要对单一数值表达式中多个占位符的，进行数值展开匹配。
                            if (placeholdersCount > 1) {
                                for (int i = 1; i < placeholdersCount; i++) {
                                    paramValueList.add(paramValue);
                                    paramValueSize++;
                                }
                            }
                        }

                        //如果数组大小大于占位符的，则对占位符展开匹配，一般默认为in运算符。
                        if (paramValueSize > placeholdersCount) {
                            int expandNum = paramValueSize - placeholdersCount + 1;
                            //此时需要做参数展开，一般情况认为是in展开。
                            paramCond = paramCond.replace("?", "?,".repeat(expandNum).substring(0, expandNum * 2 - 1));
                        }

                        //处理like查询问题。
                        if (StringUtils.containsIgnoreCase(paramCond, " like ")) {
                            boolean enableLikeQuery = queryParam.LIKE_QUERY_ENABLE();
                            //检查参数长度是否适配
                            if (enableLikeQuery) {
                                for (int i = paramValueList.size() - paramValueSize; i < paramValueList.size(); i++) {
                                    if (paramValueList.get(i) instanceof String value) {
                                        //小于最小数值的，直接移除通配符。
                                        if (value.length() <= queryParam.LIKE_QUERY_PARAM_MIN_LEN()) {
                                            enableLikeQuery = false;
                                            break;
                                        }
                                    }
                                }
                            }
                            //如果不启用like查询，则自动转成=匹配。
                            if (!enableLikeQuery) {
                                //like转成=匹配。
                                paramCond = StringUtils.replaceIgnoreCase(paramCond, " like ", "=");
                                //移除通配符，防止无法查询出数据。
                                for (int i = paramValueList.size() - paramValueSize; i < paramValueList.size(); i++) {
                                    if (paramValueList.get(i) instanceof String value) {
                                        //直接移除通配符。
                                        paramValueList.set(i, StringUtils.remove(value, '%'));
                                    }
                                }
                            }
                        }
                    }
                    sqlBuilder.append(" ").append(paramCond);
                }
            }

            //最后附加where sql。
            if (StringUtils.isNotBlank(queryParam.EXT_COND_SQL())) {
                sqlBuilder.append(" and ").append(queryParam.EXT_COND_SQL());
            }

        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        //处理排序问题。
        sqlBuilder.append(queryParam.GEN_SORT_SQL());
        return new QueryParamResult(sqlBuilder, paramValueList.toArray());
    }

    /**
     * 加载读取QueryParam的meta信息。
     *
     * @param queryParamCls QueryParam类型
     * @return List<QueryMetaInfo>
     */
    private static List<QueryMetaInfo> loadQueryParamMetaInfo(Class<?> queryParamCls) {
        return queryMetaCache.computeIfAbsent(queryParamCls.getName(), (key) -> {
            List<QueryMetaInfo> list = new ArrayList<>();
            Class<?> clazz = queryParamCls;
            for (int i = 0; clazz != Object.class && i < EntityMetaUtils.MAX_ENTITY_CLASS_EXTEND_LEVEL; clazz = clazz.getSuperclass(), i++) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    QueryMeta meta = field.getAnnotation(QueryMeta.class);
                    if (meta != null) {
                        QueryMetaInfo info = new QueryMetaInfo();
                        info.setField(field);
                        info.setQueryExpr(meta.expr());
                        list.add(info);
                    }
                }
            }
            return list;
        });
    }
}
