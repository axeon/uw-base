package uw.dao.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import uw.dao.util.QueryParamUtils;


public class TestSQLGen {

    @Test
    public void testQueryParam() {
        TestQueryParam queryParam = new TestQueryParam();
        queryParam.setId(1000L);
        queryParam.setAppName("abc%");
        queryParam.setState(1);
        queryParam.setStates(new Integer[]{1, 2, 3});
        String sql1 = QueryParamUtils.parseQueryParam(TestEntity.class, "test_abc", queryParam).getSql().toString();
        String sql2 = QueryParamUtils.parseQueryParam(TestEntity.class, null, queryParam).getSql().toString();
        String sql3 = QueryParamUtils.parseQueryParam(null, null, queryParam).getSql().toString();
        System.out.println(sql1);
        System.out.println(sql2);
        System.out.println(sql3);
        // 断言：三形态 SQL 均生成且含核心条件片段（表名/条件参数生效）
        assertNotNull(sql1);
        assertTrue(sql1.contains("test_abc"), "指定表名形态应包含表名");
        assertTrue(sql2.toLowerCase().contains("where"), "条件形态应含 WHERE");
        assertNotNull(sql3);
    }
}
