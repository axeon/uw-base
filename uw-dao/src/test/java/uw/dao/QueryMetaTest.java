package uw.dao;

import uw.common.util.SystemClock;
import uw.dao.annotation.QueryMeta;
import uw.dao.annotation.TableMeta;
import uw.dao.vo.QueryParamResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 测试查询meta
 */
public class QueryMetaTest {

    public static void main(String[] args) throws TransactionException {
        QueryParamA a = new QueryParamA();
        a.id = 1L;
        a.name = URLEncoder.encode( "JTR" , StandardCharsets.UTF_8);
        a.stateGte = 1;
        a.date = Arrays.asList( new Date[]{new Date(1), SystemClock.nowDate()} );
        a.ADD_EXT_COND( "create_date>?" ,SystemClock.nowDate());
        a.ADD_EXT_COND_SQL( "x>0" );
        a.SORT_NAME( "id");
        a.SORT_TYPE( QueryParam.SORT_ASC);
        a.ADD_SORT( "name", QueryParam.SORT_DESC);
//        a.LIKE_QUERY_ENABLE(false);
        QueryParamResult queryParamResult = DaoFactory.getInstance().parseQueryParam(EntityA.class, a);
        System.out.println(queryParamResult.getSql().toString());
//        System.out.println(Arrays.toString(queryParamResult.getParamList()));
//        System.out.println(queryParamResult.genFullSql());
    }


    public static class QueryParamA extends QueryParam {


        /**
         * 允许的排序属性。
         * key:排序名 value:排序字段
         *
         * @return
         */
        @Override
        public Map<String, String> ALLOWED_SORT_PROPERTY() {
            return new HashMap<>() {{
                put( "id", "id" );
                put( "name", "name" );
            }};
        }

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
