package uw.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * byte数组操作工具类。 期望如同操作String一样操作byte数组。
 * 默认使用big-endian编码。
 *
 * @author zhangjin
 */
public class ByteArrayUtils {

    private static final Logger logger = LoggerFactory.getLogger(ByteArrayUtils.class);

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static byte[] shortToByteArray(short num, int length) {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = (byte) (num >>> ((length - 1 - i) * 8));
        }
        return b;
    }

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static byte[] shortToByteArrayForLittleEndian(short num, int length) {
        byte[] b = new byte[length];
        for (int i = length - 1; i > 0; i--) {
            b[i] = (byte) (num >>> (i * 8));
        }
        return b;
    }

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static void shortToByteArray(short num, byte[] data, int start, int end) {
        for (int i = start; i < end; i++) {
            data[i] = (byte) (num >>> ((end - 1 - i) * 8));
        }
    }

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static void shortToByteArrayForLittleEndian(short num, byte[] data, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            data[i] = (byte) (num >>> ((i - start) * 8));
        }
    }


    /**
     * 将byte数组转化为value
     *
     * @return
     */
    public static short byteArrayToShort(byte[] b, int start, int end) {
        int mask = 0xff;
        int temp = 0;
        short res = 0;
        for (int i = start; i < end; i++) {
            res <<= 8;
            temp = b[i] & mask;
            res |= temp;
        }
        return res;
    }


    /**
     * 将byte数组转化为value
     *
     * @return
     */
    public static short byteArrayToShortForLittleEndian(byte[] b, int start, int end) {
        int mask = 0xff;
        int temp = 0;
        short res = 0;
        for (int i = end - 1; i >= start; i--) {
            res <<= 8;
            temp = b[i] & mask;
            res |= temp;
        }
        return res;
    }

    /**
     * 将byte数组转化为value
     *
     * @param b
     * @return
     */
    public static short byteArrayToShort(byte[] b) {
        return byteArrayToShort(b, 0, b.length);
    }

    /**
     * 将byte数组转化为value
     *
     * @param b
     * @return
     */
    public static short byteArrayToShortForLittleEndian(byte[] b) {
        return byteArrayToShortForLittleEndian(b, 0, b.length);
    }

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static byte[] intToByteArray(int num, int length) {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = (byte) (num >>> ((length - 1 - i) * 8));
        }
        return b;
    }

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static byte[] intToByteArrayForLittleEndian(int num, int length) {
        byte[] b = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            b[i] = (byte) (num >>> (i * 8));
        }
        return b;
    }

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static void intToByteArray(int num, byte[] data, int start, int end) {
        for (int i = start; i < end; i++) {
            data[i] = (byte) (num >>> ((end - 1 - i) * 8));
        }
    }


    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static void intToByteArrayForLittleEndian(int num, byte[] data, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            data[i] = (byte) (num >>> ((i - start) * 8));
        }
    }


    /**
     * 将byte数组转化为value
     *
     * @param b
     * @return
     */
    public static int byteArrayToInt(byte[] b) {
        return byteArrayToInt(b, 0, b.length);
    }

    /**
     * 将byte数组转化为value
     *
     * @param b
     * @return
     */
    public static int byteArrayToIntForLittleEndian(byte[] b) {
        return byteArrayToIntForLittleEndian(b, 0, b.length);
    }

    /**
     * 将byte数组转化为value
     *
     * @return
     */
    public static int byteArrayToInt(byte[] b, int start, int end) {
        int mask = 0xff;
        int temp = 0;
        int res = 0;
        for (int i = start; i < end; i++) {
            res <<= 8;
            temp = b[i] & mask;
            res |= temp;
        }
        return res;
    }

    /**
     * 将byte数组转化为value
     *
     * @return
     */
    public static int byteArrayToIntForLittleEndian(byte[] b, int start, int end) {
        int mask = 0xff;
        int temp = 0;
        int res = 0;
        for (int i = end - 1; i >= start; i--) {
            res <<= 8;
            temp = b[i] & mask;
            res |= temp;
        }
        return res;
    }

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static byte[] longToByteArray(long num, int length) {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = (byte) (num >>> ((length - 1 - i) * 8));
        }
        return b;
    }

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static byte[] longToByteArrayForLittleEndian(long num, int length) {
        byte[] b = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            b[i] = (byte) (num >>> (i * 8));
        }
        return b;
    }

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static void longToByteArray(long num, byte[] data, int start, int end) {
        for (int i = start; i < end; i++) {
            data[i] = (byte) (num >>> ((end - 1 - i) * 8));
        }
    }

    /**
     * 将value转化为byte数组
     *
     * @param num
     * @return
     */
    public static void longToByteArrayForLittleEndian(long num, byte[] data, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            data[i] = (byte) (num >>> ((i - start) * 8));
        }
    }

    /**
     * 将byte数组转化为value
     *
     * @return
     */
    public static long byteArrayToLong(byte[] b) {
        return byteArrayToLong(b, 0, b.length);
    }

    /**
     * 将byte数组转化为value
     *
     * @return
     */
    public static long byteArrayToLongForLittleEndian(byte[] b) {
        return byteArrayToLongForLittleEndian(b, 0, b.length);
    }


    /**
     * 将byte数组转化为value
     *
     * @return
     */
    public static long byteArrayToLong(byte[] b, int start, int end) {
        long temp = 0;
        long res = 0;
        for (int i = start; i < end; i++) {
            res <<= 8;
            temp = b[i] & 0xff;
            res |= temp;
        }
        return res;
    }


    /**
     * 将byte数组转化为value
     *
     * @return
     */
    public static long byteArrayToLongForLittleEndian(byte[] b, int start, int end) {
        long temp = 0;
        long res = 0;
        for (int i = end - 1; i >= start; i--) {
            res <<= 8;
            temp = b[i] & 0xff;
            res |= temp;
        }
        return res;
    }

    /**
     * 将value转化为double数组
     *
     * @param num
     * @return
     */
    public static byte[] floatToByteArray(float num, int length) {
        return intToByteArray(Float.floatToIntBits(num), length);
    }

    /**
     * 将value转化为double数组
     *
     * @param num
     * @return
     */
    public static byte[] floatToByteArrayForLittleEndian(float num, int length) {
        return intToByteArrayForLittleEndian(Float.floatToIntBits(num), length);
    }

    /**
     * 将value转化为float数组
     *
     * @param num
     * @return
     */
    public static void floatToByteArray(float num, byte[] data, int start, int end) {
        intToByteArray(Float.floatToIntBits(num), data, start, end);
    }

    /**
     * 将value转化为float数组
     *
     * @param num
     * @return
     */
    public static void floatToByteArrayForLittleEndian(float num, byte[] data, int start, int end) {
        intToByteArrayForLittleEndian(Float.floatToIntBits(num), data, start, end);
    }

    /**
     * 将byte数组转化为Double
     *
     * @return
     */
    public static float byteArrayToFloat(byte[] ba) {
        return Float.intBitsToFloat(byteArrayToInt(ba));
    }

    /**
     * 将byte数组转化为Double
     *
     * @return
     */
    public static float byteArrayToFloatForLittleEndian(byte[] ba) {
        return Float.intBitsToFloat(byteArrayToIntForLittleEndian(ba));
    }

    /**
     * 将byte数组转化为Double
     *
     * @return
     */
    public static float byteArrayToFloat(byte[] ba, int start, int end) {
        return Float.intBitsToFloat(byteArrayToInt(ba, start, end));
    }

    /**
     * 将byte数组转化为Double
     *
     * @return
     */
    public static float byteArrayToFloatForLittleEndian(byte[] ba, int start, int end) {
        return Float.intBitsToFloat(byteArrayToIntForLittleEndian(ba, start, end));
    }

    /**
     * 将value转化为double数组
     *
     * @param num
     * @return
     */
    public static byte[] doubleToByteArray(double num, int length) {
        return longToByteArray(Double.doubleToLongBits(num), length);
    }

    /**
     * 将value转化为double数组
     *
     * @param num
     * @return
     */
    public static byte[] doubleToByteArrayForLittleEndian(double num, int length) {
        return longToByteArrayForLittleEndian(Double.doubleToLongBits(num), length);
    }


    /**
     * 将value转化为double数组
     *
     * @param num
     * @return
     */
    public static void doubleToByteArray(double num, byte[] data, int start, int end) {
        longToByteArray(Double.doubleToLongBits(num), data, start, end);
    }

    /**
     * 将value转化为double数组
     *
     * @param num
     * @return
     */
    public static void doubleToByteArrayForLittleEndian(double num, byte[] data, int start, int end) {
        longToByteArrayForLittleEndian(Double.doubleToLongBits(num), data, start, end);
    }

    /**
     * 将value转化为double数组
     *
     * @param num
     * @return
     */
    public static void floatToByteArray(double num, byte[] data, int start, int end) {
        longToByteArray(Double.doubleToLongBits(num), data, start, end);
    }

    /**
     * 将value转化为double数组
     *
     * @param num
     * @return
     */
    public static void floatToByteArrayForLittleEndian(double num, byte[] data, int start, int end) {
        longToByteArrayForLittleEndian(Double.doubleToLongBits(num), data, start, end);
    }


    /**
     * 将byte数组转化为Double
     *
     * @return
     */
    public static double byteArrayToDouble(byte[] ba) {
        return Double.longBitsToDouble(byteArrayToLong(ba));
    }

    /**
     * 将byte数组转化为Double
     *
     * @return
     */
    public static double byteArrayToDoubleForLittleEndian(byte[] ba) {
        return Double.longBitsToDouble(byteArrayToLongForLittleEndian(ba));
    }

    /**
     * 将byte数组转化为Double
     *
     * @return
     */
    public static double byteArrayToDouble(byte[] ba, int start, int end) {
        return Double.longBitsToDouble(byteArrayToLong(ba, start, end));
    }

    /**
     * 将byte数组转化为Double
     *
     * @return
     */
    public static double byteArrayToDoubleForLittleEndian(byte[] ba, int start, int end) {
        return Double.longBitsToDouble(byteArrayToLongForLittleEndian(ba, start, end));
    }

    /**
     * 将二进制字符串转换成short
     *
     * @param bs
     * @return
     */
    public static short binStrToShort(String bs) {
        if (bs == null)
            return 0;
        bs = bs.trim();
        if (bs.equals(""))
            return 0;
        return Short.parseShort(bs, 2);
    }

    /**
     * 将二进制字符串转换成int
     *
     * @param bs
     * @return
     */
    public static int binStrToInt(String bs) {
        if (bs == null)
            return 0;
        bs = bs.trim();
        if (bs.equals(""))
            return 0;
        return Integer.parseInt(bs, 2);
    }

    /**
     * 将二进制字符串转换成long
     *
     * @param bs
     * @return
     */
    public static long binStrToLong(String bs) {
        if (bs == null)
            return 0;
        bs = bs.trim();
        if (bs.equals(""))
            return 0;
        return Long.parseLong(bs, 2);
    }


    /**
     * 将二进制字符串转换成byte数组。 如果字符串不能整除8，则会在后面补零
     *
     * @param bs
     * @return
     */
    public static byte[] binStrToByteArray(String bs) {
        int bsLen = bs.length();
        byte[] data = new byte[bsLen / 8];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(bs.substring(i * 8, (i + 1) * 8), 2);
        }
        return data;
    }

    /**
     * 向左侧填充指定字符，以整除
     *
     * @return
     */
    public static String padToLeftForDivide(String data, int divideSize, char c) {
        int len = data.length() % divideSize;
        if (len > 0) {
            StringBuilder sb = new StringBuilder(data.length() + divideSize);
            for (; len < divideSize; len++) {
                sb.append(c);
            }
            sb.append(data);
            data = sb.toString();
        }
        return data;
    }

    /**
     * 向右侧填充指定字符，以整除
     *
     * @return
     */
    public static String padToRightForDivide(String data, int divideSize, char c) {
        int len = data.length() % divideSize;
        if (len > 0) {
            StringBuilder sb = new StringBuilder(data.length() + divideSize);
            sb.append(data);
            for (; len < divideSize; len++) {
                sb.append(c);
            }
            data = sb.toString();
        }
        return data;
    }

    /**
     * 将byte数组转换成二进制字符串
     *
     * @param ba
     * @return
     */
    public static String byteArrayToBinStr(byte[] ba) {
        StringBuilder sb = new StringBuilder(ba.length * 10);
        String temp = null;
        for (int i = 0; i < ba.length; i++) {
            temp = Integer.toBinaryString(ba[i] & 0xff);
            for (int x = temp.length(); x < 8; x++) {
                sb.append('0');
            }
            sb.append(temp);
        }
        return sb.toString();
    }

    /**
     * 将Short转换为指定长度的BinStr
     *
     * @return
     */
    public static String shortToBinStr(short value, int len) {
        String bs = Integer.toString(value, 2);
        if (bs.length() < len) {
            for (int x = bs.length(); x < len; x++) {
                bs = '0' + bs;
            }
        } else {
            bs = bs.substring(bs.length() - len, bs.length());
        }
        return bs;
    }

    /**
     * 将Int转换为指定长度的BinStr
     *
     * @return
     */
    public static String intToBinStr(int value, int len) {
        String bs = Integer.toString(value, 2);
        if (bs.length() < len) {
            for (int x = bs.length(); x < len; x++) {
                bs = '0' + bs;
            }
        } else {
            bs = bs.substring(bs.length() - len, bs.length());
        }
        return bs;
    }

    /**
     * 将Long转换为指定长度的BinStr
     *
     * @return
     */
    public static String longToBinStr(long value, int len) {
        String bs = Long.toString(value, 2);
        for (int x = bs.length(); x < 8; x++) {
            bs = '0' + bs;
        }
        return bs;
    }

    /**
     * 构造数组。
     * 返回值可以方便的用于重构数组
     *
     * @param data
     */
    public static String byteArrayToString(byte[] data) {
        if (data == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() > 2)
                hex = hex.substring(hex.length() - 2, hex.length());
            else if (hex.length() == 1)
                hex = "0" + hex;
            sb.append("(byte)0x" + hex.toUpperCase() + ",");
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 打印bytes
     *
     * @param data
     */
    public static String byteArrayToStringForView(byte[] data) {
        if (data == null)
            return "";
        int size = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() > 2)
                hex = hex.substring(hex.length() - 2, hex.length());
            else if (hex.length() == 1)
                hex = "0" + hex;
            sb.append(hex.toUpperCase());
            size++;
            if (size == 31) {
                size = 0;
                sb.append(" \n");
            } else {
                size++;
                sb.append(" ");
            }
        }
        return sb.toString();
    }


    /**
     * 按照给出Byte[]的顺序，相加所有byte
     *
     * @param args
     * @return
     */
    public static byte[] combine(byte[]... args) {
        int length = 0, pos = 0;
        // 先获得数组总大小
        for (int i = 0; i < args.length; i++) {
            length += args[i].length;
        }
        byte[] sum = new byte[length];
        // 循环copy
        for (int i = 0; i < args.length; i++) {
            System.arraycopy(args[i], 0, sum, pos, args[i].length);
            pos += args[i].length;
        }
        return sum;
    }

    /**
     * 从指定的byte数组中移出类似sub一样的内容
     *
     * @param array
     * @param sub
     * @return
     */
    public static byte[] remove(byte[] array, byte[] sub) {
        int pos = -1;
        while ((pos = indexOf(array, 0, sub)) > -1) {
            byte[] dst = new byte[array.length - sub.length];
            System.arraycopy(array, 0, dst, 0, pos);
            System.arraycopy(array, pos + sub.length, dst, pos, dst.length - pos);
            array = dst;
        }
        return array;
    }

    /**
     * 替换byte数组
     *
     * @param data
     * @param src
     * @param dist
     * @return
     */
    public static byte[] replace(byte[] data, byte[] src, byte[] dist) {
        int pos = -1, start = 0;
        while ((pos = indexOf(data, 0, src)) > -1) {
            byte[] dst = new byte[data.length - src.length + dist.length];
            System.arraycopy(data, 0, dst, 0, pos);
            System.arraycopy(dist, 0, dst, pos, dist.length);
            System.arraycopy(data, pos + src.length, dst, pos + dist.length, data.length - pos - dist.length);
            data = dst;
        }
        return data;
    }

    /**
     * 检查两个数组是否完全相等
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean deepEquals(byte[] a, byte[] b) {
        if (a.length != b.length)
            return false;
        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i])
                return false;
        return true;

    }

    /**
     * 取数组中指定部分
     *
     * @param array
     * @param start
     * @param end
     * @return
     */
    public static byte[] subArray(byte[] array, int start, int end) {
        byte[] ret = new byte[end - start];
        System.arraycopy(array, start, ret, 0, ret.length);
        return ret;
    }

    /**
     * 判定位置。
     *
     * @param array
     * @param sub
     * @return
     */
    public static int indexOf(byte[] array, int start, byte[] sub) {
        for (int i = start; i < array.length; i++) {
            if (array[i] == sub[0]) {
                boolean flag = true;
                if (array.length - i < sub.length) {
                    flag = false;
                } else {
                    for (int k = 1; k < sub.length; k++)
                        if (array[i + k] != sub[k]) {
                            flag = false;
                        }
                }
                if (flag)
                    return i;
                else
                    continue;
            }
        }
        return -1;
    }

    /**
     * 判定位置。
     *
     * @param array
     * @param sub
     * @return
     */
    public static int lastIndexOf(byte[] array, int start, byte[] sub) {
        for (int i = start; i > -1; i--) {
            if (array[i] == sub[sub.length - 1]) {
                boolean flag = true;
                if (i < sub.length) {
                    flag = false;
                } else {
                    for (int k = 1; k < sub.length; k++)
                        if (array[i - k] != sub[sub.length - 1 - k]) {
                            flag = false;
                        }
                }
                if (flag)
                    return i - sub.length + 1;
                else
                    continue;
            }

        }
        return -1;
    }

    /**
     * 数组是否以sub部分结束
     *
     * @param array
     * @param sub
     * @return
     */
    public static boolean endWith(byte[] array, byte[] sub) {
        return lastIndexOf(array, 0, sub) + sub.length == array.length;
    }

    /**
     * 数组是否以sub部分起始
     *
     * @param array
     * @param sub
     * @return
     */
    public static boolean startWith(byte[] array, byte[] sub) {
        return indexOf(array, 0, sub) == 0;
    }

    /**
     * 从尾部算起，计算有 多少个0x00。
     *
     * @param data
     * @return
     */
    public static int countNullFromEnd(byte[] data) {
        int count = 0;
        for (int i = data.length - 1; i > 0; i--) {
            if (data[i] != 0x00)
                break;
            count++;
        }
        return count;
    }

    /**
     * 把一个16禁止表示的0x字符串转换为byte数组。
     */
    public static byte[] hexToByteArray(String str) {
        if (str.length() % 2 != 0)
            return new byte[0];
        str = str.replaceAll("0x", "").replaceAll("0X", "");
        byte[] data = new byte[str.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16);
        }
        return data;
    }


    public static void main(String[] args) {
        System.out.println("new String(new byte[]{"+ ByteArrayUtils.byteArrayToString("s".getBytes())+"})");
        System.out.println((int)'*');
    }

}
