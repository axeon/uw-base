package uw.common.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.lang.reflect.*;
import java.util.*;

/**
 * kryo基础工具类。
 */
public class KryoUtils {

    private static final Logger log = LoggerFactory.getLogger(KryoUtils.class);
    /**
     * 默认池子容量16。
     */
    private static final int CAPACITY = 32;

    /**
     * kryo池。
     */
    private static final Pool<Kryo> kryoPool = new Pool<Kryo>(true, true, CAPACITY) {
        protected Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
            kryo.setRegistrationRequired(false);
            kryo.setReferences(false);
            kryo.setOptimizedGenerics(true);
            // 预注册常用 JDK 类
            kryo.register(ArrayList.class);
            kryo.register(LinkedList.class);
            kryo.register(HashMap.class);
            kryo.register(LinkedHashMap.class);
            kryo.register(TreeMap.class);
            kryo.register(HashSet.class);
            kryo.register(LinkedHashSet.class);
            kryo.register(Date.class);
            return kryo;
        }
    };

    /**
     * 输出池。没有搞input pull的原因是已经拿到了完整数组，没有重用价值。
     */
    private static final Pool<Output> outputPool = new Pool<Output>(true, true, CAPACITY) {
        protected Output create() {
            return new Output(2560, -1);
        }
    };

    /**
     * 序列化。
     *
     * @param value
     * @return
     */
    public static byte[] serialize(Object value) {
        if (value == null) {
            return null;
        }
        byte[] data = null;
        final Kryo kryo = kryoPool.obtain();
        final Output output = outputPool.obtain();
        try {
            kryo.writeObject(output, value);
            //此时复制出数据
            data = output.toBytes();
        } finally {
            outputPool.free(output);
            kryoPool.free(kryo);
        }
        return data;
    }

    /**
     * 序列化到输出流。
     *
     * @param value
     * @return
     */
    public static void serialize(Object value, OutputStream out) {
        if (value == null) {
            return;
        }
        final Kryo kryo = kryoPool.obtain();
        final Output output = outputPool.obtain();
        try {
            output.setOutputStream(out);// 绑定流
            kryo.writeObject(output, value);
            output.flush();
        } finally {
            output.setOutputStream(null);// ← 解绑！
            outputPool.free(output);
            kryoPool.free(kryo);
        }
    }

    /**
     * 反序列化。
     *
     * @param data
     * @return
     */
    public static <T> T deserialize(byte[] data, Class<T> cls) {
        if (data == null || data.length == 0) {
            return null;
        }
        T value;
        final Kryo kryo = kryoPool.obtain();
        final Input input = new Input(data);
        try {
            value = kryo.readObject(input, cls);
        } finally {
            kryoPool.free(kryo);
        }
        return value;
    }

    /**
     * 把type类型转为class类型。
     *
     * @param type
     * @return
     */
    public static Class<?> type2Class(Type type) {
        if (type instanceof Class<?> cls) {
            return resolveConcreteClass(cls);
        } else if (type instanceof GenericArrayType) {
            // having to create an array instance to get the class is kinda nasty
            // but apparently this is a current limitation of java-reflection concerning array classes.
            return Array.newInstance(type2Class(((GenericArrayType) type).getGenericComponentType()), 0).getClass(); // E.g. T[] -> T -> Object.class if <T> or Number.class
        } else if (type instanceof ParameterizedType parameterizedType) {
            return resolveConcreteClass(type2Class(parameterizedType.getRawType()));
        } else if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length == 0 ? Object.class : type2Class(bounds[0]); // erasure is to the left-most bound.
        } else if (type instanceof WildcardType) {
            Type[] bounds = ((WildcardType) type).getUpperBounds();
            return bounds.length == 0 ? Object.class : type2Class(bounds[0]); // erasure is to the left-most upper bound.
        } else {
            // throw new UnsupportedOperationException( "cannot handle type class: " + type.getClass() );
            return Object.class;
        }
    }

    /**
     * 获取实际类型。
     * @param cls
     * @return
     */
    private static Class<?> resolveConcreteClass(Class<?> cls) {
        if (cls == List.class || cls == AbstractList.class) {
            return ArrayList.class;
        }
        if (cls == Set.class || cls == AbstractSet.class) {
            return HashSet.class;
        }
        if (cls == SortedSet.class || cls == NavigableSet.class) {
            return TreeSet.class;
        }
        if (cls == Map.class || cls == AbstractMap.class) {
            return HashMap.class;
        }
        if (cls == SortedMap.class || cls == NavigableMap.class) {
            return TreeMap.class;
        }
        if (cls == Deque.class || cls == Queue.class) {
            return LinkedList.class;
        }
        return cls;
    }

    public static void main(String[] args) {
//        FusionCache.Config config = FusionCache.Config.builder().cacheName( "test" ).build();
//        byte[] data = serialize( config );
//        System.out.println( data.length );
//        System.out.println( new String( data ) );
//        System.out.println( deserialize( data , CacheProtectedValue.class) );
//        ArrayList<Long> list = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            list.add((long) i);
//        }
//        byte[] data = serialize(list);
//        System.out.println(data.length);
//        System.out.println(new String(data));
//        System.out.println(deserialize(data, ArrayList.class));
//        ArrayList<FusionCache.Config> list = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            list.add( FusionCache.Config.builder().cacheName( "test" + i ).build());
//        }
//        byte[] data = serialize( list);
//        System.out.println( data.length );
//        System.out.println( new String( data ) );
//        System.out.println( deserialize( data, ArrayList.class ) );
//        byte[] data = serialize( 1000000 );
//        System.out.println( data.length );
//        System.out.println( new String( data ) );
//        System.out.println( deserialize( data,Long.class) );
//        CacheValueWrapper<FusionCache.Config> wrapper = new CacheValueWrapper<FusionCache.Config>(FusionCache.Config.builder().cacheName("test").build(),100000);
//        byte[] data = serializeCacheValue(wrapper);
//        System.out.println(data.length);
//        System.out.println(new String(data));
//        wrapper = deserializeCacheValue(data, FusionCache.Config.class);
//        System.out.println(JsonUtils.toString(wrapper));
//        System.out.println(SystemClock.now());

    }

}
