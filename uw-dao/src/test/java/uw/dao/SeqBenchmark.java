package uw.dao;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import uw.dao.sequence.DaoSequenceFactory;
import uw.dao.sequence.FusionSequenceFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput})//基准测试类型
@OutputTimeUnit(TimeUnit.SECONDS)//基准测试结果的时间类型
@Warmup(iterations = 1, time = 3)//预热的迭代次数
@Threads(200)//测试线程数量
@State(Scope.Benchmark)//该状态为每个线程独享
@Measurement(iterations = 100, time = 3)
public class SeqBenchmark {
    private static final Logger log = LoggerFactory.getLogger( SeqBenchmark.class );
    private ConfigurableApplicationContext context;
    private static Map<Long,String> idMap = new ConcurrentHashMap();

    private static final String seqKey = "test8";

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include( SeqBenchmark.class.getSimpleName() )
                .forks( 3)
                .build();
        new Runner( opt ).run();
    }

//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.INLINE)
//    public static void daoSeq() {
//        DaoSequenceFactory.getSequenceId( seqKey );
//    }

//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.INLINE)
//    public static void fusionSeq() {
//        FusionSequenceFactory.getSequenceId( seqKey );
//    }

//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.INLINE)
//    public static void seq() {
//        long seq = SequenceFactory.getSequenceId( seqKey );
//    }

//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.INLINE)
//    public static void checkFusionSeq() {
//        long seq = FusionSequenceFactory.getSequenceId( seqKey );
//        if (idMap.containsKey( seq )){
//            log.warn( "FusionSequenceFactory seq[{}]和[{}]冲突！",seq,idMap.get( seq ) );
//        }else{
//            idMap.put( seq,"FusionSequenceFactory");
//        }
//    }

//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.INLINE)
//    public static void checkDaoSeq() {
//        long seq = DaoSequenceFactory.getSequenceId( seqKey );
//        if (idMap.containsKey( seq )){
//            log.warn( "DaoSequenceFactory seq[{}]和[{}]冲突！",seq,idMap.get( seq ) );
//        }else{
//            idMap.put( seq,"DaoSequenceFactory");
//        }
//    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void checkMixedSeq() {
        long seq = FusionSequenceFactory.getSequenceId( seqKey );
        if (idMap.containsKey( seq )){
            log.warn( "FusionSequenceFactory seq[{}]和[{}]冲突！",seq,idMap.get( seq ) );
        }else{
            idMap.put( seq,"FusionSequenceFactory");
        }
        seq = DaoSequenceFactory.getSequenceId( seqKey );
        if (idMap.containsKey( seq )){
            log.warn( "DaoSequenceFactory seq[{}]和[{}]冲突！",seq,idMap.get( seq ) );
        }else{
            idMap.put( seq,"DaoSequenceFactory");
        }
    }


    @Setup
    public void init() {
        context = SpringApplication.run( UwDaoTestApplication.class );
    }


}
