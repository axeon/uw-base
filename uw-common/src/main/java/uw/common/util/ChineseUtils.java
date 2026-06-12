package uw.common.util;

import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

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
     * 用一维数组优化空间复杂度从O(mn)降到O(n)。
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
     * 用一维数组优化空间复杂度从O(mn)降到O(n)。
     *
     * @param strA 字符串A
     * @param strB 字符串B
     * @return 相似度，范围0-10000
     */
    public static int lcsSimilarDegree(String strA, String strB) {
        int maxLen = Math.max(strA.length(), strB.length());
        if (maxLen == 0) {
            return 10000;
        }
        // 确保内层循环用较短的字符串，减少空间
        if (strA.length() < strB.length()) {
            String tmp = strA;
            strA = strB;
            strB = tmp;
        }
        char[] a = strA.toCharArray();
        char[] b = strB.toCharArray();
        int m = a.length;
        int n = b.length;
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a[i - 1] == b[j - 1]) {
                    curr[j] = prev[j - 1] + 1;
                } else {
                    curr[j] = Math.max(curr[j - 1], prev[j]);
                }
            }
            int[] t = prev;
            prev = curr;
            curr = t;
        }
        return Math.round(prev[n] * 10000.0f / maxLen);
    }

    /**
     * 基于N-gram余弦相似度的计算，返回0-10000。
     * 将字符串拆为二元组(bigram)，通过向量夹角余弦衡量相似度。
     * 相比LCS，对少量字符差异更敏感，更适合区分"名称高度相似但实际不同"的景区。
     *
     * @param strA 字符串A
     * @param strB 字符串B
     * @return 相似度，范围0-10000
     */
    public static int ngramSimilarDegree(String strA, String strB) {
        if (strA.equals(strB)) {
            return 10000;
        }
        int maxLen = Math.max(strA.length(), strB.length());
        if (maxLen <= 1) {
            return strA.equals(strB) ? 10000 : 0;
        }
        Map<String, int[]> bigrams = new HashMap<>();
        // 统计strA的bigram频率
        for (int i = 0; i < strA.length() - 1; i++) {
            String gram = strA.substring(i, i + 2);
            bigrams.computeIfAbsent(gram, k -> new int[2])[0]++;
        }
        // 统计strB的bigram频率
        for (int i = 0; i < strB.length() - 1; i++) {
            String gram = strB.substring(i, i + 2);
            bigrams.computeIfAbsent(gram, k -> new int[2])[1]++;
        }
        // 计算余弦相似度
        long dotProduct = 0, normA = 0, normB = 0;
        for (int[] freq : bigrams.values()) {
            dotProduct += (long) freq[0] * freq[1];
            normA += (long) freq[0] * freq[0];
            normB += (long) freq[1] * freq[1];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        double cosine = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return (int) Math.round(cosine * 10000);
    }

    /**
     * 半角转全角
     *
     * @param input String.
     * @return 全角字符串.
     */
    public static String toSBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == ' ') {
                c[i] = '\u3000';
            } else if (c[i] < '\177') {
                c[i] = (char) (c[i] + 65248);

            }
        }
        return new String(c);
    }

    /**
     * 全角转半角
     *
     * @param input String.
     * @return 半角字符串
     */
    public static String toDBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == '\u3000') {
                c[i] = ' ';
            } else if (c[i] > '\uFF00' && c[i] < '\uFF5F') {
                c[i] = (char) (c[i] - 65248);

            }
        }
        return new String(c);
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
