package uw.common.util;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 手工 kryo 序列化 {@link AuthTokenData}（方式二：手工读写）。
 * <p>
 * 通过 {@link KryoUtils#write(int, java.util.function.Consumer)} / {@link KryoUtils#read(byte[], java.util.function.Function)}
 * 的轻量回调手工写读原语，字段顺序与 {@link AuthTokenData2} 的 write/read 保持一致，保证三种方式可比。
 *
 * @author axeon
 */
public class KryoAuthTokenSerializer {

    /** token序列化结果约200字节，512留足余量且不放大GC。 */
    private static final int BUFFER_SIZE = 512;

    private KryoAuthTokenSerializer() {
    }

    /**
     * 手工序列化。
     *
     * @param data token数据
     * @return 字节数组
     */
    public static byte[] serialize(AuthTokenData data) {
        if (data == null) {
            return new byte[0];
        }
        return KryoUtils.serialize(BUFFER_SIZE, output -> writeToken(output, data));
    }

    /**
     * 手工反序列化。
     *
     * @param bytes 字节数组
     * @return token数据
     */
    public static AuthTokenData deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return KryoUtils.deserialize(bytes, input -> readToken(input));
    }

    private static void writeToken(Output output, AuthTokenData data) {
        output.writeVarLong(data.expireAt, true);
        output.writeVarLong(data.createAt, true);
        output.writeVarInt(data.tokenType, true);
        output.writeVarLong(data.saasId, true);
        output.writeVarInt(data.userType, true);
        output.writeVarLong(data.userId, true);
        output.writeVarLong(data.mchId, true);
        output.writeVarLong(data.groupId, true);
        output.writeVarInt(data.userGrade, true);
        output.writeVarInt(data.isMaster, true);
        output.writeString(data.userName == null ? "" : data.userName);
        output.writeString(data.nickName == null ? "" : data.nickName);
        output.writeString(data.realName == null ? "" : data.realName);
        output.writeString(data.mobile == null ? "" : data.mobile);
        output.writeString(data.email == null ? "" : data.email);
        output.writeString(data.userIp == null ? "" : data.userIp);
        Set<Integer> permSet = data.permSet;
        output.writeVarInt(permSet == null ? 0 : permSet.size(), true);
        if (permSet != null) {
            for (int v : permSet) {
                output.writeVarInt(v, true);
            }
        }
        Map<String, String> configMap = data.configMap;
        output.writeVarInt(configMap == null || configMap.isEmpty() ? 0 : configMap.size(), true);
        if (configMap != null) {
            for (Map.Entry<String, String> kv : configMap.entrySet()) {
                output.writeString(kv.getKey());
                output.writeString(kv.getValue());
            }
        }
    }

    private static AuthTokenData readToken(Input input) {
        AuthTokenData data = new AuthTokenData();
        data.expireAt = input.readVarLong(true);
        data.createAt = input.readVarLong(true);
        data.tokenType = input.readVarInt(true);
        data.saasId = input.readVarLong(true);
        data.userType = input.readVarInt(true);
        data.userId = input.readVarLong(true);
        data.mchId = input.readVarLong(true);
        data.groupId = input.readVarLong(true);
        data.userGrade = input.readVarInt(true);
        data.isMaster = input.readVarInt(true);
        data.userName = input.readString();
        data.nickName = input.readString();
        data.realName = input.readString();
        data.mobile = input.readString();
        data.email = input.readString();
        data.userIp = input.readString();
        int permSize = input.readVarInt(true);
        if (permSize > 0) {
            Set<Integer> set = new HashSet<>(permSize);
            for (int i = 0; i < permSize; i++) {
                set.add(input.readVarInt(true));
            }
            data.permSet = set;
        }
        int configSize = input.readVarInt(true);
        if (configSize > 0) {
            Map<String, String> map = new HashMap<>(configSize);
            for (int i = 0; i < configSize; i++) {
                map.put(input.readString(), input.readString());
            }
            data.configMap = map;
        }
        return data;
    }
}
