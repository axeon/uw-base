package uw.dao;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import uw.dao.conf.DaoConfig;
import uw.dao.conf.DaoConfigManager;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

/**
 * mydbProxy的benchmark。
 */
@BenchmarkMode({Mode.Throughput})//基准测试类型
@OutputTimeUnit(TimeUnit.SECONDS)//基准测试结果的时间类型
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Threads(100)//测试线程数量
@State(Scope.Benchmark)//该状态为每个线程独享
//度量:iterations进行测试的轮次，time每轮进行的时长，timeUnit时长单位,batchSize批次数量
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
public class MydbProxyBenchmark {

    static final DaoFactory dao = DaoFactory.getInstance();
    private static final String LOAD_SEQ = "select seq_id,increment_num from sys_seq where seq_name=? ";

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include( MydbProxyBenchmark.class.getSimpleName() )
                .forks( 0 )
                .build();
        new Runner( opt ).run();
    }

    @Setup
    public static void setup() {
        DaoConfig daoConfig = new DaoConfig();
        DaoConfig.ConnPool pool = new DaoConfig.ConnPool();
        DaoConfig.ConnPoolConfig poolConfig = new DaoConfig.ConnPoolConfig();
        poolConfig.setDriver( "com.mysql.cj.jdbc.Driver" );
        poolConfig.setUrl( "jdbc:mysql://192.168.88.21:3300/uw_auth?characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true" );
        poolConfig.setUsername( "root" );
        poolConfig.setPassword( "mysqlRootPassword123" );
        poolConfig.setMinConn( 10 );
        poolConfig.setMaxConn( 100 );
        poolConfig.setConnMaxAge( 3600 );
        poolConfig.setConnBusyTimeout( 120 );
        poolConfig.setConnIdleTimeout( 120 );
        pool.setRoot( poolConfig );
        pool.setList( new LinkedHashMap<>() );
        daoConfig.setConnPool( pool );
        DaoConfigManager.setConfig( daoConfig );
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void getSeq() throws TransactionException {
        dao.queryForDataSet( LOAD_SEQ, new Object[]{"SysCritLog"} );
    }

}
