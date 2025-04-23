package uw.dao.impl;

import org.junit.jupiter.api.Test;
import uw.dao.util.QueryParamUtils;


public class TestSQLGen {

    @Test
    public void testQueryParam() {
        TestQueryParam queryParam = new TestQueryParam();
        queryParam.setId(1000L);
        queryParam.setAppName("abc%");
        queryParam.setState(1);
        queryParam.setStates(new Integer[]{1, 2, 3});
        System.out.println(QueryParamUtils.parseQueryParam(TestEntity.class, "test_abc", queryParam).getSql());
        System.out.println(QueryParamUtils.parseQueryParam(TestEntity.class, null, queryParam).getSql());
        System.out.println(QueryParamUtils.parseQueryParam(null, null, queryParam).getSql());

    }
}
