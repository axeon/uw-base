package uw.auth.service.ipblock;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.List;

/**
 * Ip匹配管理器。
 */
public class IpMatchHelper {

    /**
     * 通用Ip匹配缓存。
     */
    private final LoadingCache<String, List<IpRange>> commonIpMatcherCache;

    /**
     * 构造器，cacheNum指定缓存数量。
     *
     * @param cacheNum
     */
    public IpMatchHelper(int cacheNum) {
        commonIpMatcherCache = Caffeine.newBuilder()
                .maximumSize( cacheNum )
                .build( new CacheLoader<String, List<IpRange>>() {
                    @Override
                    public List<IpRange> load(String key) throws Exception {
                        if (key == null)
                            return null;
                        return IpMatchUtils.sortList( key.split( "," ) );
                    }
                } );
    }

    /**
     * 检查是否匹配。
     *
     * @param ips
     * @param ip
     * @return
     */
    public boolean matches(String ips, String ip) {
        List<IpRange> ipRanges = commonIpMatcherCache.get( ips );
        if (ipRanges != null) {
            return IpMatchUtils.matches( ipRanges, ip );
        } else {
            return false;
        }
    }

}
