package uw.dao;

import uw.dao.annotation.QueryMeta;
import uw.dao.annotation.TableMeta;
import uw.dao.vo.QueryParamResult;

import java.util.Arrays;
import java.util.Date;

/**
 * 测试查询meta
 */
public class QueryMetaTest {

    public static void main(String[] args) throws TransactionException {
        QueryParamA a = new QueryParamA();
        a.id = 1L;
        a.name = "test";
        a.date = new Date[]{new Date(1), new Date()};
        QueryParamResult queryParamResult = DaoFactory.getInstance().parseQueryParam(EntityA.class, a);
        System.out.println(queryParamResult.getSql().toString());
        System.out.println(Arrays.toString(queryParamResult.getParamList()));
        System.out.println(queryParamResult.genFullSql());
    }


    public static class QueryParamA extends QueryParam {

        @QueryMeta(expr = "id=?")
        private Long id;

        @QueryMeta(expr = "name like ?")
        private String name;

        @QueryMeta(expr = "date between ? and ? order by id asc")
        private Date[] date;

    }

    public static class PageQueryParamA extends PageQueryParam {
        @QueryMeta(expr = "id=?")
        private long id;

        @QueryMeta(expr = "name like '?%'")
        private String name;

        @QueryMeta(expr = "date between ? and ?")
        private Date[] date;
    }

    @TableMeta(sql = "select * from tab_a")
    public static class EntityA {

    }


}
