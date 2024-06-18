package uw.cache.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.FusionCache;
import uw.cache.vo.FusionCacheNotifyMessage;

import java.util.ArrayList;

/**
 * kryo序列化工具类。
 */
public class KryoUtils {

    private static final Logger log = LoggerFactory.getLogger( KryoUtils.class );
    /**
     * 默认池子容量16。
     */
    private static final int CAPACITY = 16;

    /**
     * kryo池。
     */
    private static final Pool<Kryo> kryoPool = new Pool<Kryo>( true, true, CAPACITY ) {
        protected Kryo create() {
            final Kryo kryo = new Kryo();
            kryo.setReferences( false );
            kryo.setRegistrationRequired( false );
            return kryo;
        }
    };
    /**
     * 输出池。没有搞input pull的原因是已经拿到了完整数组，没有重用价值。
     */
    private static final Pool<Output> outputPool = new Pool<Output>( true, true, CAPACITY ) {
        protected Output create() {
            return new Output( 4096, -1 );
        }
    };

    /**
     * 序列化。
     *
     * @param value
     * @return
     */
    public static byte[] serialize(Object value) {
        byte[] data = null;
        final Kryo kryo = kryoPool.obtain();
        final Output output = outputPool.obtain();
        try {
            kryo.writeObject( output, value );
            output.flush();
            //此时复制出数据
            data = output.toBytes();
        } catch (Throwable e) {
            log.error( "kryo deserialize error! {}", e.getMessage(), e );
        } finally {
            output.reset();
            outputPool.free( output );
            kryoPool.free( kryo );
        }
        return data;
    }

    /**
     * 反序列化。
     *
     * @param data
     * @return
     */
    public static <T> T deserialize(byte[] data, Class<T> cls) {
        T value = null;
        final Kryo kryo = kryoPool.obtain();
        final Input input = new Input( data );
        try {
            value = kryo.readObject( input, cls );
        } catch (Throwable e) {
            log.error( "kryo deserialize error! {}", e.getMessage(), e );
        } finally {
            kryoPool.free( kryo );
        }
        return value;
    }

//    public static void main(String[] args) {
//        FusionCache.Config config = FusionCache.Config.builder().cacheName( "test" ).build();
//        byte[] data = serialize( config );
//        System.out.println( data.length );
//        System.out.println( new String( data ) );
//        System.out.println( deserialize( data , FusionCache.Config.class) );
//        ArrayList<FusionCache.Config> list = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            list.add( FusionCache.Config.builder().cacheName( "test" + i ).build());
//        }
//        byte[] data = serialize( list );
//        System.out.println( data.length );
//        System.out.println( new String( data ) );
//        System.out.println( deserialize( data, ArrayList.class ) );
//        byte[] data = serialize( 1000000 );
//        System.out.println( data.length );
//        System.out.println( new String( data ) );
//        System.out.println( deserialize( data,Long.class) );
//    }

}
