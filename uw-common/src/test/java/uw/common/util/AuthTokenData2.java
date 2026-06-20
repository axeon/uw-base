package uw.common.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 测试用 token 数据，实现 {@link KryoSerializable}，由 kryo 在序列化时回调 {@link #write}/{@link #read} 手工读写原语。
 * <p>
 * 字段与 {@link AuthTokenData} 完全一致，手工读写顺序与 KryoAuthTokenSerializer 的手工版保持一致，
 * 保证三种方式（对象读写 / 手工读写 / KryoSerializable）的体积与性能可比。
 * <p>
 * 与"手工读写"方式的区别：手工读写由调用方在 KryoUtils.write/read 的回调里写原语（对象本身是普通 POJO）；
 * 而本类把读写逻辑内聚到对象自身的 write/read，kryo 自动回调，调用方仍用 {@code KryoUtils.serialize/deserialize}。
 *
 * @author axeon
 */
public class AuthTokenData2 implements KryoSerializable {

    public int tokenType;
    public long saasId;
    public int userType;
    public long userId;
    public long mchId;
    public long groupId;
    public int isMaster;
    public String userName;
    public String realName;
    public String nickName;
    public String mobile;
    public String email;
    public String userIp;
    public int userGrade;
    public long expireAt;
    public long createAt;
    public Set<Integer> permSet;
    public Map<String, String> configMap;

    public AuthTokenData2() {
    }

    /**
     * 构建一个内容齐全的测试 token，字段值与 {@link AuthTokenData#buildFull()} 完全一致。
     *
     * @return 测试数据
     */
    public static AuthTokenData2 buildFull() {
        AuthTokenData2 data = new AuthTokenData2();
        data.tokenType = 2;
        data.saasId = 10086L;
        data.userType = 2;
        data.userId = 9876543210L;
        data.mchId = 5001L;
        data.groupId = 200L;
        data.isMaster = 1;
        data.userGrade = 5;
        data.userName = "axeon";
        data.nickName = "axeon_nickname";
        data.realName = "张三";
        data.mobile = "13800138000";
        data.email = "axeon@example.com";
        data.userIp = "192.168.1.100";
        data.createAt = 1716000000000L;
        data.expireAt = 1716003600000L;

        Set<Integer> permSet = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            permSet.add(1000 + i);
        }
        data.permSet = permSet;

        Map<String, String> configMap = new HashMap<>();
        configMap.put("theme", "dark");
        configMap.put("lang", "zh-CN");
        configMap.put("timezone", "Asia/Shanghai");
        configMap.put("maxConn", "100");
        data.configMap = configMap;
        return data;
    }

    @Override
    public void write(Kryo kryo, Output output) {
        output.writeVarLong(expireAt, true);
        output.writeVarLong(createAt, true);
        output.writeVarInt(tokenType, true);
        output.writeVarLong(saasId, true);
        output.writeVarInt(userType, true);
        output.writeVarLong(userId, true);
        output.writeVarLong(mchId, true);
        output.writeVarLong(groupId, true);
        output.writeVarInt(userGrade, true);
        output.writeVarInt(isMaster, true);
        output.writeString(userName == null ? "" : userName);
        output.writeString(nickName == null ? "" : nickName);
        output.writeString(realName == null ? "" : realName);
        output.writeString(mobile == null ? "" : mobile);
        output.writeString(email == null ? "" : email);
        output.writeString(userIp == null ? "" : userIp);
        // 权限集合：permId恒非负，空集合用size=0表达，size与权限值统一走正数变长编码
        output.writeVarInt(permSet == null ? 0 : permSet.size(), true);
        if (permSet != null) {
            for (int v : permSet) {
                output.writeVarInt(v, true);
            }
        }
        // 配置Map：为空时写0标记，否则先写数量
        output.writeVarInt(configMap == null || configMap.isEmpty() ? 0 : configMap.size(), true);
        if (configMap != null) {
            for (Map.Entry<String, String> kv : configMap.entrySet()) {
                output.writeString(kv.getKey());
                output.writeString(kv.getValue());
            }
        }
    }

    @Override
    public void read(Kryo kryo, Input input) {
        expireAt = input.readVarLong(true);
        createAt = input.readVarLong(true);
        tokenType = input.readVarInt(true);
        saasId = input.readVarLong(true);
        userType = input.readVarInt(true);
        userId = input.readVarLong(true);
        mchId = input.readVarLong(true);
        groupId = input.readVarLong(true);
        userGrade = input.readVarInt(true);
        isMaster = input.readVarInt(true);
        userName = input.readString();
        nickName = input.readString();
        realName = input.readString();
        mobile = input.readString();
        email = input.readString();
        userIp = input.readString();
        int permSize = input.readVarInt(true);
        if (permSize > 0) {
            Set<Integer> set = new HashSet<>(permSize);
            for (int i = 0; i < permSize; i++) {
                set.add(input.readVarInt(true));
            }
            permSet = set;
        }
        int configSize = input.readVarInt(true);
        if (configSize > 0) {
            Map<String, String> map = new HashMap<>(configSize);
            for (int i = 0; i < configSize; i++) {
                map.put(input.readString(), input.readString());
            }
            configMap = map;
        }
    }
}
