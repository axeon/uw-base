package uw.auth.service.ratelimit;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.auth.service.annotation.RateLimitDeclare;
import uw.auth.service.constant.RateLimitTarget;
import uw.auth.service.token.AuthTokenData;
import uw.httpclient.exception.DataMapperException;
import uw.httpclient.json.JsonInterfaceHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 限速工具类。
 */
public class RateLimitUtils {


    private static final Logger log = LoggerFactory.getLogger( RateLimitUtils.class );


    /**
     * 默认限速。
     */
    private static RateLimitConfig defaultConfig;

    /**
     * 设置默认的速率控制器
     *
     * @param defaultConfig
     */
    public static void setDefaultConfig(RateLimitConfig defaultConfig) {
        RateLimitUtils.defaultConfig = defaultConfig;
    }

    /**
     * 根据url 匹配合适的RateLimit对象。
     *
     * @param authToken
     * @param url
     * @return 如果匹配不到，返回null
     */
    public static RateLimitInfo match(AuthTokenData authToken, RateLimitDeclare rateLimitDeclare, String ip, String url) {
        //匹配config
        RateLimitConfig config = null;
        List<RateLimitConfig> list = authToken.getTokenPerm().getRateLimits();
        if (list != null) {
            for (RateLimitConfig conf : list) {
                //匹配全局默认值
                if (StringUtils.equals( conf.getUrl(), url )) {
                    config = conf;
                    break;
                } else if (StringUtils.equals( conf.getUrl(), "*" )) {
                    config = conf;
                    break;
                }
            }
        }
        int requests;
        int seconds;
        RateLimitTarget target;
        //1.如果有配置文件，则进行精准匹配。
        //2.否则进行注解匹配。
        //3.尝试*匹配
        //4.使用全局匹配！
        if (config != null) {
            //精准匹配
            if (!config.getUrl().equals( "*" )) {
                //尝试匹配config
                requests = config.getRequests();
                seconds = config.getSeconds();
                target = config.getTarget();
            } else {
                if (rateLimitDeclare != null) {
                    requests = rateLimitDeclare.requests();
                    seconds = rateLimitDeclare.seconds();
                    target = rateLimitDeclare.target();
                } else {
                    //尝试匹配config
                    requests = config.getRequests();
                    seconds = config.getSeconds();
                    target = config.getTarget();
                }
            }
        } else {
            if (rateLimitDeclare != null) {
                requests = rateLimitDeclare.requests();
                seconds = rateLimitDeclare.seconds();
                target = rateLimitDeclare.target();
            } else {
                if (defaultConfig != null) {
                    //使用默认配置
                    requests = defaultConfig.getRequests();
                    seconds = defaultConfig.getSeconds();
                    target = defaultConfig.getTarget();
                } else {
                    return null;
                }
            }
        }

        //如果参数不合法，直接返回null
        if (requests == 0 || seconds == 0 || target == RateLimitTarget.NONE) {
            return null;
        }
        //生成限速信息。
        RateLimitInfo info = new RateLimitInfo();
        info.setRequests( requests );
        info.setSeconds( seconds );
        info.setTarget( target );
        switch (target) {
            case IP:
                info.setUri( ip );
                break;
            case USER:
                info.setUri( authToken.getUserId() + "@" + authToken.getSaasId() );
                break;
            case MCH:
                info.setUri( authToken.getMchId() + "|" + authToken.getSaasId() );
                break;
            case SAAS:
                info.setUri( "*@" + authToken.getSaasId() );
                break;
            case USER_URI:
                info.setUri( authToken.getUserId() + "@" + authToken.getSaasId() + ":" + url );
                break;
            case MCH_URI:
                info.setUri( authToken.getMchId() + "|" + authToken.getSaasId() + ":" + url );
                break;
            case SAAS_URI:
                info.setUri( "*@" + authToken.getSaasId() + ":" + url );
                break;
        }
        return info;
    }


    /**
     * 合并限速配置。
     * 如果list有base中没有的元素，则新增。
     *
     * @param baseList 基础数据
     * @param addList
     * @return 合并后的列表。
     */
    public static List<RateLimitConfig> merge(List<RateLimitConfig> baseList, List<RateLimitConfig> addList) {
        if (baseList == null) {
            return addList;
        }
        if (addList == null) {
            return baseList;
        }
        ArrayList result = new ArrayList( baseList );
        for (RateLimitConfig conf : addList) {
            if (findConfig( baseList, conf.getUrl() ) == null) {
                result.add( conf );
            }
        }
        return result;
    }

    /**
     * 使用rateLimit字符串来表达。
     * 格式为 limitTarget:limitValue/limitTimeSeconds;
     * 如果limitTarget=*，则为全局限速。
     *
     * @param rateLimitJson
     */
    public static List<RateLimitConfig> parse(String rateLimitJson) {
        List<RateLimitConfig> list = null;
        try {
            list = JsonInterfaceHelper.JSON_CONVERTER.parse( rateLimitJson, new TypeReference<List<RateLimitConfig>>() {
            } );
        } catch (DataMapperException e) {
            log.error( e.getMessage(), e );
        }
        return list;
    }

    /**
     * 从列表中查找config。
     *
     * @param list
     * @param url
     * @return
     */
    private static RateLimitConfig findConfig(List<RateLimitConfig> list, String url) {
        if (list != null) {
            for (RateLimitConfig conf : list) {
                if (conf.getUrl().equals( url )) {
                    return conf;
                }
            }
        }
        return null;
    }

}
