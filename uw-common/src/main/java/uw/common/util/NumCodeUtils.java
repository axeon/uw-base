package uw.common.util;


import java.util.Random;

/**
 * 数字编码工具类。
 */
public class NumCodeUtils {


    /**
     * 数字编码映射表。
     */
    private static final char[] NUM_ENC_MAP = new char[]{'8', '7', '0', '5', '3', '4', '1', '6', '9', '2'};

    /**
     * 数字解码映射表。
     */
    private static final char[] NUM_DEC_MAP = new char[]{'2', '6', '9', '4', '5', '3', '7', '1', '0', '8'};

    /**
     * 数字0的ASCII码。
     */
    private static final int ZERO_ASCII = '0';

    /**
     * 重映射数字。
     *
     * @param num    需要重映射的数字。
     * @param numMap 数字Map表。
     * @return
     */
    public static char[] remapNum(char[] num, char[] numMap) {
        for (int i = 0; i < num.length; i++) {
            num[i] = numMap[num[i] - ZERO_ASCII];
        }
        return num;
    }

    /**
     * 混淆数字。
     * 应特别注意，混淆后的数字可能会导致0前置！！！
     *
     * @param num
     * @return
     */
    public static char[] confuseNum(char[] num) {
        if (num.length < 2) {
            return num;
        }
        //混淆掩码。掩码为1-9。
        int mask = (num[num.length - 1] % 9) + 1;
        if (mask > num.length) {
            mask = mask % num.length;
        } else if (mask == num.length) {
            mask = 1;
        }
        char[] numEnc = new char[num.length];
        numEnc[0] = num[num.length - 1];
        int copyLen = num.length - mask;
        //前后位置对调。
        System.arraycopy( num, 0, numEnc, copyLen, mask );
        System.arraycopy( num, mask, numEnc, 1, copyLen - 1 );
        return numEnc;
    }

    /**
     * 恢复混淆数字。
     *
     * @param numEnc
     * @return
     */
    public static char[] clarifyNum(char[] numEnc) {
        if (numEnc.length < 2) {
            return numEnc;
        }
        //混淆掩码。掩码为1-9。
        int mask = (numEnc[0] % 9) + 1;
        if (mask > numEnc.length) {
            mask = mask % numEnc.length;
        } else if (mask == numEnc.length) {
            mask = 1;
        }
        char[] num = new char[numEnc.length];
        num[num.length - 1] = numEnc[0];
        int copyLen = numEnc.length - mask;
        //前后位置对调。
        System.arraycopy( numEnc, 1, num, mask, copyLen - 1 );
        System.arraycopy( numEnc, copyLen, num, 0, mask );
        return num;
    }


//    public static void main(String[] args) {
    //制作编码map表。
//        char[] NUM_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
//        char[] NUM_ENC_MAP = shuffleChars( NUM_CHARS );
//        char[] NUM_DEC_MAP = reverseCharMap( NUM_ENC_MAP );
//        System.out.println( printCode( "NUM_ENC_MAP", NUM_ENC_MAP ) );
//        System.out.println( printCode( "NUM_DEC_MAP", NUM_DEC_MAP ) );
    //基础测试
//        long id = 123456789012345678L;
//        long idEnc = remapNum( id, NUM_ENC_MAP );
//        System.out.println( id );
//        System.out.println( idEnc );
//        System.out.println( remapNum( idEnc, NUM_DEC_MAP ) );
    //查重测试
//        for (int i = 123456789; i < 1000000000; i++){
//            System.out.println(remapNum( i, NUM_ENC_MAP ));
//        }
//        long id = 123456789012345678L;
//        long idEnc = Long.parseLong( String.valueOf( remapNum( String.valueOf( id ).toCharArray(), NUM_ENC_MAP ) ) );
//        long idDec = Long.parseLong( String.valueOf( remapNum( String.valueOf( idEnc ).toCharArray(), NUM_DEC_MAP ) ));
//        long idEnc = Long.parseLong( String.valueOf( confuseNum( String.valueOf( id ).toCharArray() ) ) );
//        long idDec = Long.parseLong( String.valueOf( clarifyNum( String.valueOf( idEnc ).toCharArray() ) ) );
//        long idEnc = Long.parseLong( String.valueOf( confuseNum( remapNum( String.valueOf( id ).toCharArray(), NUM_ENC_MAP ) ) ) );
//        long idDec = Long.parseLong( String.valueOf( remapNum( clarifyNum( String.valueOf( idEnc ).toCharArray() ), NUM_DEC_MAP ) ) );
//        System.out.println( id );
//        System.out.println( idEnc );
//        System.out.println( idDec );
    //排查编码错误
//        System.out.println( "开始排查编码错误。" );
//        long now = SystemClock.now();
//        long numBase = 123;
//        for (int i = 0; i < 1000; i++) {
//            new String( confuseNum( remapNum( String.valueOf( numBase + i ).toCharArray(), NUM_ENC_MAP ) ) );
//        }
//        Set<Long> ids = new HashSet<>();
//        for (int i = 0; i < 100000000; i++) {
//            new String( confuseNum( remapNum( String.valueOf( numBase + i ).toCharArray(), NUM_ENC_MAP ) ) );
//            long dataEnc = Long.parseLong( new String(confuseNum( remapNum( String.valueOf( numBase + i ).toCharArray(), NUM_ENC_MAP ) )));
//            long dataEnc = Long.parseLong( "1"+new String(confuseNum( remapNum( String.valueOf( numBase + i ).toCharArray(), NUM_ENC_MAP ) )));
//            if (ids.contains( dataEnc )){
//                System.out.println( "编码重复，原始数据："+(numBase+i)+"，编码后数据："+ dataEnc);
//            }else {
//                ids.add( dataEnc );
//            }
//            long data = Long.parseLong( String.valueOf( remapNum(clarifyNum(String.valueOf( dataEnc ).substring( 1 ).toCharArray() ), NUM_DEC_MAP) ) );
//            if (data!=(numBase+i)){
//                System.out.println( "编码异常，原始数据："+(numBase+i));
//            }
//        }
//        System.out.println( "排查编码错误结束，耗时：" + (SystemClock.now() - now) );
//    }

    /**
     * 随机打乱字符数组。
     *
     * @param numChars
     */
    private static char[] shuffleChars(char[] numChars) {
        Random random = new Random();
        for (int i = numChars.length - 1; i > 0; i--) {
            int j = random.nextInt( i + 1 );
            char temp = numChars[i];
            numChars[i] = numChars[j];
            numChars[j] = temp;
        }
        return numChars;
    }

    /**
     * 反转字符MAP。
     */
    private static char[] reverseCharMap(char[] numMap) {
        char[] numReverseMap = new char[numMap.length];
        for (int i = 0; i < numMap.length; i++) {
            numReverseMap[numMap[i] - '0'] = (char) (i + '0');
        }
        return numReverseMap;
    }

    /**
     * 打印字符数组。
     *
     * @param name
     * @param numChars
     * @return
     */
    private static String printCode(String name, char[] numChars) {
        StringBuilder sb = new StringBuilder();
        sb.append( "private static final char[] " ).append( name ).append( " = new char[]{" );
        for (char c : numChars) {
            sb.append( "'" ).append( c ).append( "'," );
        }
        sb.deleteCharAt( sb.length() - 1 );
        sb.append( "};" );
        return sb.toString();
    }

}
