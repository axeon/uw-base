package uw.common.util;

import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 汉字工具类。
 *
 * */
public class ChineseUtils {

    private static final Logger log = LoggerFactory.getLogger(ChineseUtils.class);


    /**
     * 转换为拼音字符串。
     *
     * @param str
     * @param separator
     * @param pinyinFormat
     * @return
     * @throws PinyinException
     */
    public static String convertToPinyin(String str, String separator, PinyinFormat pinyinFormat) throws PinyinException {
        return PinyinHelper.convertToPinyinString(str, separator, pinyinFormat);
    }

    /**
     * 转换为拼音字符串。
     *
     * @param str
     * @param separator
     * @return
     * @throws PinyinException
     */
    public static String convertToPinyin(String str, String separator) throws PinyinException {
        return PinyinHelper.convertToPinyinString(str, separator);
    }

    /**
     * 多音字检测。
     *
     * @param c
     * @return
     */
    public static boolean hasMultiPinyin(char c) {
        return PinyinHelper.hasMultiPinyin(c);
    }

    /**
     * 获得拼音缩写。
     *
     * @param str
     * @return
     * @throws PinyinException
     */
    public static String getShortPinyin(String str) throws PinyinException {
        return PinyinHelper.getShortPinyin(str);
    }


    /**
     * 基于ngram的相似度计算，返回0-10000。
     * 算法实现见 {@link StringTools#ngramSimilarity}。
     *
     * @param strA 字符串A
     * @param strB 字符串B
     * @return 相似度，范围0-10000
     */
    public static int similarDegree(String strA, String strB) {
        return ngramSimilarDegree(strA, strB);
    }

    /**
     * 基于LCS(最长公共子序列)的相似度计算，返回0-10000。
     * 算法实现见 {@link StringTools#lcsSimilarity}。
     *
     * @param strA 字符串A
     * @param strB 字符串B
     * @return 相似度，范围0-10000
     */
    public static int lcsSimilarDegree(String strA, String strB) {
        return (int) Math.round(StringTools.lcsSimilarity(strA, strB) * 10000);
    }

    /**
     * 基于N-gram余弦相似度的计算，返回0-10000。
     * 将字符串拆为二元组(bigram)，通过向量夹角余弦衡量相似度。
     * 相比LCS，对少量字符差异更敏感，更适合区分"名称高度相似但实际不同"的景区。
     * 算法实现见 {@link StringTools#ngramSimilarity}。
     *
     * @param strA 字符串A
     * @param strB 字符串B
     * @return 相似度，范围0-10000
     */
    public static int ngramSimilarDegree(String strA, String strB) {
        return (int) Math.round(StringTools.ngramSimilarity(strA, strB) * 10000);
    }


//    public static void main(String[] args) {
//        String[][] cases = {
//                // 简称 vs 全称（应高匹配）
//                {"故宫", "北京故宫博物院"},
//                {"长隆", "广州长隆欢乐世界"},
//                // 完全相同（应100%）
//                {"黄果树瀑布", "黄果树瀑布"},
//                // 同音异字（应中等匹配）
//                {"黄果树", "黄菓树"},
//                // 高度相似但不同（应低匹配）
//                {"武汉桔子水晶酒店(西站)", "武汉桔子水晶酒店(东站)"},
//                {"武汉万达嘉华酒店", "武汉万达瑞华酒店"},
//                {"北京全聚德烤鸭店", "北京全聚德烤鸭店东四分店"},
//                // 完全不同（应极低匹配）
//                {"故宫", "长城"},
//                {"黄果树瀑布", "九寨沟"},
//        };
//
//        System.out.printf("%-30s %-30s %8s %8s%n", "字符串A", "字符串B", "LCS", "N-gram");
//        System.out.println("-".repeat(80));
//
//        for (String[] c : cases) {
//            int lcs = ChineseUtils.lcsSimilarDegree(c[0], c[1]);
//            int ngram = ChineseUtils.ngramSimilarDegree(c[0], c[1]);
//            System.out.printf("%-30s %-30s %7d%% %7d%%%n", c[0], c[1], lcs / 100, ngram / 100);
//        }
//    }
}
