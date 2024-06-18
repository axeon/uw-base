package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * "total": {
 * "value": 10793698,
 * "relation": "eq"
 * }
 *
 * 
 * @since 2019-12-16
 **/
@JsonIgnoreProperties(ignoreUnknown = true)
public class Total {

    private int value;

    private String relation;

    @JsonProperty("value")
    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @JsonProperty("relation")
    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }
}
