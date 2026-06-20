package uw.common.util;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 测试用 token 数据（POJO，字段与 uw-auth-service.AuthTokenData 对齐）。
 * <p>
 * 用于对比三种 kryo 序列化方式（对象读写 / 手工读写 / KryoSerializable）的大小与性能。
 * 字段设为 public 便于测试代码直接访问，保留无参构造供 kryo 反射实例化。
 *
 * @author axeon
 */
public class AuthTokenData {

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

    public AuthTokenData() {
    }

    /**
     * 构建一个内容齐全的测试 token（含权限集与配置Map）。
     *
     * @return 测试数据
     */
    public static AuthTokenData buildFull() {
        AuthTokenData data = new AuthTokenData();
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
}
