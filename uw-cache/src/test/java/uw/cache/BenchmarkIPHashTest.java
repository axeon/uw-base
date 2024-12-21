package uw.cache;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput})//基准测试类型
@OutputTimeUnit(TimeUnit.MILLISECONDS)//基准测试结果的时间类型
@Warmup(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)//预热的迭代次数
@Threads(20)//测试线程数量
@State(Scope.Benchmark)//该状态为每个线程独享
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS, batchSize = -1)
public class BenchmarkIPHashTest {

    public static final String IP_ADDRESS = "192.168.88.21";

    public BenchmarkIPHashTest() {
    }

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder().include( BenchmarkIPHashTest.class.getSimpleName() ).forks( 1 ).build();
        new Runner( opts ).run();
    }

    @Setup
    public void init() {

    }

    @Benchmark
    public void testJdkHash() {
        IP_ADDRESS.hashCode();
    }

    @Benchmark
    public void testIpSplit() {
        ipToLongSplit( IP_ADDRESS );
    }

    @Benchmark
    public void testIpIndexOf() {
        ipToLongIndexOf( IP_ADDRESS );
    }

    @TearDown
    public void down() {
    }

    /**
     * 将字符串类型的ip转成long。
     *
     * @param ipStr
     * @return
     */
    private static long ipToLongSplit(String ipStr) {
        String[] ips = ipStr.split( "\\." );
        long[] ip = new long[4];
        if (ips.length != 4) {
            return -1;
        }
        try {
            ip[0] = Long.parseLong( ips[0] );
            ip[1] = Long.parseLong( ips[1] );
            ip[2] = Long.parseLong( ips[2] );
            ip[3] = Long.parseLong( ips[3] );
        } catch (Exception e) {
            ip = new long[4];
        }
        // 将每个.之间的字符串转换成整型
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
    }

    /**
     * 将字符串类型的ip转成long。
     *
     * @param ipStr
     * @return
     */
    private static long ipToLongIndexOf(String ipStr) {
        int pos1 = ipStr.indexOf( "." );
        int pos2 = ipStr.indexOf( ".", pos1 + 1 );
        int pos3 = ipStr.indexOf( ".", pos2 + 1 );
        if (pos1 == -1 || pos2 == -1 || pos3 == -1 || pos1 > 15 || pos2 > 15 || pos3 > 15) {
            return -1;
        }
        long ip = Long.parseLong( ipStr.substring( 0, pos1 ) ) << 24;
        ip += Long.parseLong( ipStr.substring( pos1 + 1, pos2 ) ) << 16;
        ip += Long.parseLong( ipStr.substring( pos2 + 1, pos3 ) ) << 8;
        ip += Long.parseLong( ipStr.substring( pos3 + 1 ) );
        return ip;

    }

}
