package uw.ai;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import uw.ai.conf.UwAiProperties;

/**
 * AiClientHelper。
 */
public class AiClientHelper {

    private static final Logger log = LoggerFactory.getLogger( AiClientHelper.class );
    /**
     * Rest模板类
     */
    private static RestTemplate tokenRestTemplate;

    private static UwAiProperties uwAiProperties;

    public AiClientHelper(UwAiProperties uwAiProperties, RestTemplate tokenRestTemplate) {
        AiClientHelper.tokenRestTemplate = tokenRestTemplate;
        AiClientHelper.uwAiProperties = uwAiProperties;
    }


}
