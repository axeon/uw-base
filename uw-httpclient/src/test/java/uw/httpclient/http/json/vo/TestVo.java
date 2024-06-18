package uw.httpclient.http.json.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @since 2018-03-01
 */
public class TestVo {

    private String name;

    private int age;

    private String address;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("age")
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @JsonProperty("address")
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
