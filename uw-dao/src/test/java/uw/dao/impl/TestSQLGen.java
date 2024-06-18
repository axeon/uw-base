package uw.dao.impl;

import org.apache.commons.lang3.StringUtils;
import org.openjdk.jmh.annotations.*;
import uw.dao.util.QueryParamUtils;
import uw.dao.vo.QueryParamResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput})//基准测试类型
@OutputTimeUnit(TimeUnit.SECONDS)//基准测试结果的时间类型
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS, batchSize = 1)//预热的迭代次数
@Threads(3)//测试线程数量
@State(Scope.Benchmark)//该状态为每个线程独享
//度量:iterations进行测试的轮次，time每轮进行的时长，timeUnit时长单位,batchSize批次数量
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS, batchSize = 3)
public class TestSQLGen {

//    public static void main(String[] args) throws RunnerException {
//        Options opt = new OptionsBuilder()
//                .include(TestSQLGen.class.getSimpleName())
//                .forks(1)
//                .build();
//        new Runner(opt).run();
//    }

    public static void main(String[] args) {
        QueryParamResult result = testQueryParam();

        System.out.println("参数化SQL: "+result.getSql());
        System.out.println("参数列表: "+Arrays.toString(result.getParamList()));
        System.out.println("完整SQL: "+result.genFullSql());

        System.out.println("占位符数量: "+StringUtils.countMatches(result.getSql(),"?"));
        System.out.println("参数数量: "+result.getParamList().length);
    }


    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static QueryParamResult testQueryParam(){
        TestQueryParam queryParam = new TestQueryParam();
        queryParam.setId(1000L);
        queryParam.setAppName("abc%");
        queryParam.setState(1);
        queryParam.setStates(new Integer[]{1, 2, 3});
        List stateList = new ArrayList<>();
        stateList.add(1);
        stateList.add(2);
        queryParam.setAppInfo("%test%");
        queryParam.setCreateDate(new Date[]{new Date(System.currentTimeMillis()-86400000),new Date()});
        queryParam.setStateList(stateList);
        queryParam.setStateOp("!=999");
        queryParam.setStateOps(new String[]{">=1","<=100"});
        queryParam.setStateOn(true);
        queryParam.ADD_EXT_PARAM("and test>?",1);
        queryParam.ADD_EXT_PARAM("and test>? and test<?",new int[]{2,3});
        queryParam.ADD_EXT_PARAM("and test in (?)",new int[]{7,8,9});
//        queryParam.SET_LIKE_QUERY_ENABLE(false);
//        queryParam.SET_LIKE_QUERY_PARAM_MIN_LEN(10);
        QueryParamResult result = QueryParamUtils.parseQueryParam(TestEntity.class, null, queryParam);
        return result;
    }
}
