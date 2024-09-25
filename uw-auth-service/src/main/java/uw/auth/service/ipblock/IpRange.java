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

    private long start;

    private long end;

    public IpRange() {
    }

    public IpRange(String ipPattern) {
        convert( ipPattern );
    }

    public IpRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    /**
     * ipPattern转换
     *
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
                maskBitCount = IPV4_BIT_COUNT - (StringUtils.countMatches( ipPattern, "*" ) * 8L);
                ip = ipPattern.replaceAll( "\\*", "255" );
            }

            String[] splitIps = ip.split( "\\." );
            for (String splitIp : splitIps) {
                start = start << 8;
                start |= Long.parseLong( splitIp );
            }

            end = start;
            long mask = 0xFFFFFFFFL >>> maskBitCount;

            end = end | mask;
            start = start & (~mask);
        } catch (Exception e) {
            log.warn( "非法的ip格式: {}", ipPattern );
        }
    }

}
