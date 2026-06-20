package uw.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 四种 kryo 序列化方式的数据大小对比：
 * <ol>
 *   <li><b>对象读写</b>：{@link KryoUtils#serialize(Object)} 反射读写整个 POJO（kryo FieldSerializer）；</li>
 *   <li><b>手工读写</b>（lambda）：{@link KryoUtils#serialize(int, java.util.function.Consumer)} 回调里手工写原语（{@link KryoAuthTokenSerializer}）；</li>
 *   <li><b>KryoSerializable</b>：对象实现 {@link com.esotericsoftware.kryo.KryoSerializable}，
 *       用 {@link KryoUtils#serialize(Object)}，由 kryo 回调对象自身的 write/read；</li>
 *   <li><b>KryoData 接口式</b>：对象实现 {@link uw.common.data.KryoData}，
 *       用 {@link KryoUtils#serializeData(KryoData)} / {@link KryoUtils#deserializeData(byte[], uw.common.data.KryoData)}。</li>
 * </ol>
 * 字段值与读写顺序四者对齐，差异主要来自字段编码细节（反射 FieldSerializer vs 手工原语）。
 *
 * <h3>实测结果（同一份 token 数据）</h3>
 * <pre>
 * 对象读写(反射)      : 188 bytes
 * 手工读写(lambda)    : 185 bytes
 * KryoSerializable    : 185 bytes
 * KryoData(接口式)    : 185 bytes
 * </pre>
 * 三种手工方式（lambda / KryoSerializable / KryoData）使用同一套原语编码，产出完全相同（185 字节），
 * 比反射 FieldSerializer（188）小 3 字节。
 *
 * @author axeon
 */
class KryoSerializeSizeTest {

    @Test
    void compareSize() {
        AuthTokenData data1 = AuthTokenData.buildFull();
        AuthTokenData2 data2 = AuthTokenData2.buildFull();
        AuthTokenData3 data3 = AuthTokenData3.buildFull();

        // 方式一：对象读写（反射）
        byte[] objBytes = KryoUtils.serialize(data1);
        AuthTokenData objRound = KryoUtils.deserialize(objBytes, AuthTokenData.class);
        assertNotNull(objRound);
        assertEquals(data1.userId, objRound.userId);

        // 方式一：对象读写,带class信息（反射）
        byte[] objBytesWithClass = KryoUtils.serializeWithClass(data1);
        AuthTokenData objRoundWithClass = (AuthTokenData)KryoUtils.deserializeWithClass(objBytesWithClass);
        assertNotNull(objRoundWithClass);
        assertEquals(data1.userId, objRoundWithClass.userId);

        // 方式二：手工读写（lambda）
        byte[] manualBytes = KryoAuthTokenSerializer.serialize(data1);
        AuthTokenData manualRound = KryoAuthTokenSerializer.deserialize(manualBytes);
        assertNotNull(manualRound);
        assertEquals(data1.userId, manualRound.userId);
        assertEquals(data1.permSet, manualRound.permSet);
        assertEquals(data1.configMap, manualRound.configMap);

        // 方式三：KryoSerializable
        byte[] ksBytes = KryoUtils.serialize(data2);
        AuthTokenData2 ksRound = KryoUtils.deserialize(ksBytes, AuthTokenData2.class);
        assertNotNull(ksRound);
        assertEquals(data2.userId, ksRound.userId);
        assertEquals(data2.permSet, ksRound.permSet);
        assertEquals(data2.configMap, ksRound.configMap);

        // 方式四：KryoData 接口式
        byte[] kdBytes = KryoUtils.serializeData(data3);
        AuthTokenData3 kdRound = KryoUtils.deserializeData(kdBytes, new AuthTokenData3());
        assertNotNull(kdRound);
        assertEquals(data3.userId, kdRound.userId);
        assertEquals(data3.permSet, kdRound.permSet);
        assertEquals(data3.configMap, kdRound.configMap);

        System.out.println("============================================================");
        System.out.println("四种 kryo 序列化方式大小对比（同一份 token 数据）");
        System.out.println("  对象读写(反射)      : " + objBytes.length + " bytes");
        System.out.println("  对象读写(带class信息): " + objBytesWithClass.length + " bytes");
        System.out.println("  手工读写(lambda)    : " + manualBytes.length + " bytes");
        System.out.println("  KryoSerializable    : " + ksBytes.length + " bytes");
        System.out.println("  KryoData(接口式)    : " + kdBytes.length + " bytes");
        System.out.println("============================================================");
    }
}
