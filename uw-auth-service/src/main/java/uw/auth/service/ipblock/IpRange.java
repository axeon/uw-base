package uw.auth.service.ipblock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IP Range对象。
 */
public class IpRange {

    private static final Logger log = LoggerFactory.getLogger( IpRange.class );

    private static final int IPV4_BIT_COUNT = 32;

    private long from;

    private long to;

    public IpRange() {
    }

    public IpRange(String ipPattern) {
        convert( ipPattern );
    }

    public IpRange(long from, long to) {
        this.from = from;
        this.to = to;
    }

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    /**
     * ipPattern转换
     *
     * @param ipPattern
     */
    private void convert(String ipPattern) {
        try {
            String ip = ipPattern.trim();
            long maskBitCount = IPV4_BIT_COUNT;
            if (ipPattern.indexOf( "/" ) > 0) {
                String[] addressAndMask = ipPattern.split( "/" );
                ip = addressAndMask[0];
                maskBitCount = Long.parseLong( addressAndMask[1] );
            } else if (ipPattern.indexOf( '*' ) > 0) {
                maskBitCount = IPV4_BIT_COUNT - (StringUtils.countMatches( ipPattern, "*" ) * 8);
                ip = ipPattern.replaceAll( "\\*", "255" );
            }

            String[] splitIps = ip.split( "\\." );
            for (String splitIp : splitIps) {
                from = from << 8;
                from |= Long.parseLong( splitIp );
            }

            to = from;
            long mask = 0xFFFFFFFFL >>> maskBitCount;

            to = to | mask;
            from = from & (~mask);
        } catch (Exception e) {
            log.warn( "非法的ip格式: {}", ipPattern );
        }
    }

}
