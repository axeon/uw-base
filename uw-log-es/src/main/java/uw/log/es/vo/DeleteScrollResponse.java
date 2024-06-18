package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {
 * "succeeded": true,
 * "num_freed": 3
 * }
 *
 * 
 * @since 2019-12-16
 **/
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteScrollResponse {


    private boolean succeeded;

    private int numFreed;


    public boolean isSucceeded() {
        return succeeded;
    }

    @JsonProperty("succeeded")
    public boolean getSucceeded() {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    @JsonProperty("num_freed")
    public int getNumFreed() {
        return numFreed;
    }

    public void setNumFreed(int numFreed) {
        this.numFreed = numFreed;
    }
}
