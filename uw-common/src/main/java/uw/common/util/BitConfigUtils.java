package uw.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 用位存储开关状态，可以支持最多32个开关。
 */
public class BitConfigUtils {

    /**
     * 是否已打开某个开关
     *
     * @param configType
     * @return
     */
    public final static boolean isOn(int config, int configType) {
        return (config & (0x1 << configType)) != 0;
    }

    /**
     * 打开某个开关
     *
     * @param configType
     */
    public final static int on(int config, int configType) {
        return config | (1 << configType);
    }

    /**
     * 打开某些开关
     *
     * @param configTypes
     */
    public final static int on(int config, int... configTypes) {
        for (int configType : configTypes) {
            config |= (1 << configType);
        }
        return config;
    }

    /**
     * 关闭某个开关
     *
     * @param configType
     */
    public final static int off(int config, int configType) {
        if (isOn( config, configType )) {
            return config ^ (1 << configType);
        }
        return config;
    }

    /**
     * 是否已打开某个开关
     *
     * @param configType
     * @return
     */
    public final static boolean isOn(long config, int configType) {
        return (config & (0x1L << configType)) != 0;
    }

    /**
     * 打开某个开关
     *
     * @param configType
     */
    public final static long on(long config, int configType) {
        return config | (1L << configType);
    }

    /**
     * 打开某些开关
     *
     * @param configTypes
     */
    public final static long on(long config, int... configTypes) {
        for (int configType : configTypes) {
            config |= (1L << configType);
        }
        return config;
    }

    /**
     * 关闭某个开关
     *
     * @param configType
     */
    public final static long off(long config, int configType) {
        if (isOn( config, configType )) {
            return config ^ (1L << configType);
        }
        return config;
    }

    /**
     * 计算已经打开的开关位数。
     *
     * @param config
     * @return
     */
    public final static int countOn(long config) {
        int count = 0;
        while (config != 0) {
            config = config & (config - 1);
            count++;
        }
        return count;
    }

    /**
     * 计算已经打开的开关位数。
     *
     * @param config
     * @return
     */
    public final static int countOn(int config) {
        int count = 0;
        while (config != 0) {
            config = config & (config - 1);
            count++;
        }
        return count;
    }

    /**
     * 计算打开的位置。
     *
     * @param config
     * @return
     */
    public static List<Integer> calcOnPosList(long config) {
        List<Integer> posList = new ArrayList<>();
        int bitPos = 0;
        while (config != 0) {
            if ((config & 0x01) != 0) { // 检查最低位是否为1
                posList.add( bitPos ); // 将当前位的位置添加到列表的开头
            }
            config >>>= 1; // 将 num 右移一位
            bitPos++;
        }
        return posList;
    }
//
//    public static void main(String[] args) {
//        long config = on( 0L, 0, 1, 2, 3, 6, 7, 8, 20, 21, 22, 61, 62, 63 );
//        System.out.println( calcOnPosList( config ) );
//    }


}
