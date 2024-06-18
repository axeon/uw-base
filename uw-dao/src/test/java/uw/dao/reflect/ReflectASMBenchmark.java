//评测结果：reflectasm在访问filed模式下，比java17原生缓存的filed性能低20%左右。
//package uw.dao.reflect;
//
//import com.esotericsoftware.reflectasm.FieldAccess;
//
//import java.lang.reflect.Field;
//import java.util.concurrent.TimeUnit;
//
//import org.openjdk.jmh.annotations.*;
//import org.openjdk.jmh.runner.Runner;
//import org.openjdk.jmh.runner.RunnerException;
//import org.openjdk.jmh.runner.options.Options;
//import org.openjdk.jmh.runner.options.OptionsBuilder;
//
///**
// * @author Frapples <isfrapples@outlook.com>
// * @date 18-11-6
// */
//
//@BenchmarkMode(Mode.Throughput)
//@OutputTimeUnit(TimeUnit.MICROSECONDS)
//@Warmup(iterations = 1,time=3,timeUnit = TimeUnit.SECONDS)//预热的迭代次数
//@Threads(8)//测试线程数量
//@State(Scope.Thread)//该状态为每个线程独享
////度量:iterations进行测试的轮次，time每轮进行的时长，timeUnit时长单位,batchSize批次数量
//@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
//public class ReflectASMBenchmark {
//
//    private Point p;
//
//    Field fx,fy;
//
//    FieldAccess fieldAccess;
//
//    @Setup
//    public void init() throws  IllegalAccessException, NoSuchFieldException {
//        p = new Point();
//        fx = p.getClass().getDeclaredField("x");
//        fx.setAccessible(true);
//        fy = p.getClass().getDeclaredField("y");
//        fy.setAccessible(true);
//        fieldAccess = FieldAccess.get(p.getClass());
//    }
//
//    @Benchmark
//    public void reflectSet() throws Throwable {
//        fx.set(p,1);
//        fy.set(p,2);
//    }
//
//    @Benchmark
//    public void reflectGet() throws Throwable {
//        fx.get(p);
//        fy.get(p);
//    }
//
//    @Benchmark
//    public void reflectAsmSet() throws Throwable {
//        fieldAccess.set(p,"x",1);
//        fieldAccess.set(p,"y",2);
//    }
//
//    @Benchmark
//    public void reflectAsmGet() throws Throwable {
//        fieldAccess.get(p,"x");
//        fieldAccess.get(p,"y");
//    }
//
////    @Benchmark
////    public void javaSet() throws Throwable {
////        p.setX(1);
////        p.setY(2);
////    }
////
////    @Benchmark
////    public void javaGet() throws Throwable {
////        p.getX();
////        p.getY();
////    }
//
//
//    public static void main(String[] args) throws RunnerException {
//        Options opt = new OptionsBuilder()
//                .include(ReflectASMBenchmark.class.getName())
//                .forks(0)
//                .build();
//
//        new Runner(opt).run();
//    }
//
//
//}