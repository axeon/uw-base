package uw.dao;

import uw.dao.annotation.QueryMeta;
import uw.dao.annotation.TableMeta;
import uw.dao.vo.QueryParamResult;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 测试查询meta
 */
public class QueryMetaTest {

    public static void main(String[] args) throws TransactionException {
        QueryParamA a = new QueryParamA();
        a.id = 1L;
        a.name = URLEncoder.encode( "JTR" );
        a.stateGte = 1;
        a.date = Arrays.asList( new Date[]{new Date(1), new Date()} );
        a.ADD_EXT_PARAM( "create_date>?" ,new Date());
        a.ADD_EXT_WHERE_SQL( " x>0" );
//        a.LIKE_QUERY_ENABLE(false);
        QueryParamResult queryParamResult = DaoFactory.getInstance().parseQueryParam(EntityA.class, a);
        System.out.println(queryParamResult.getSql().toString());
        System.out.println(Arrays.toString(queryParamResult.getParamList()));
        System.out.println(queryParamResult.genFullSql());
    }


    public static class QueryParamA extends QueryParam {

        @QueryMeta(expr = "id=?")
        private Long id;

        @QueryMeta(expr = "name LIKE ? or desc LIKE ?")
        private String name;

        @QueryMeta(expr = "date between ? and ?")
        private List<Date> date;

        @QueryMeta(expr = "state>=?")
        private int stateGte;

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
