package uw.httpclient.http.json.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON 序列化/反序列化测试用的简单 VO。
 * <p>
 * 用于 {@link uw.httpclient.http.json.JsonInterfaceTest} 验证 {@link uw.httpclient.json.JsonObjectMapperImpl}
 * 的 round-trip（对象 → JSON → 对象）正确性。字段刻意覆盖 String / int 两种基础类型。
 *
 * @since 2018-03-01
 */
public class TestVo {

    /**
     * 名称。
     */
    private String name;

    /**
     * 年龄。
     */
    private int age;

    /**
     * 地址。
     */
    private String address;

    /**
     * 获取名称。
     *
     * @return 名称。
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * 设置名称。
     *
     * @param name 名称。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取年龄。
     *
     * @return 年龄。
     */
    @JsonProperty("age")
    public int getAge() {
        return age;
    }

    /**
     * 设置年龄。
     *
     * @param age 年龄。
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * 获取地址。
     *
     * @return 地址。
     */
    @JsonProperty("address")
    public String getAddress() {
        return address;
    }

    /**
     * 设置地址。
     *
     * @param address 地址。
     */
    public void setAddress(String address) {
        this.address = address;
    }
}
