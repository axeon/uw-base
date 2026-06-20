package uw.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link MaskUtils} 单元测试。
 */
public class MaskUtilsTest {

    // ==================== 通用 mask（固定掩码串） ====================

    @Test
    void testMaskBasic() {
        Assertions.assertEquals("138****5678", MaskUtils.mask("13812345678", 3, 4));
        Assertions.assertEquals("ab****yz", MaskUtils.mask("abcdefghijxyz", 2, 2));
    }

    @Test
    void testMaskCustomMask() {
        Assertions.assertEquals("ab----yz", MaskUtils.mask("abcdefghijxyz", 2, 2, "----"));
        // mask 为 null 时回退默认 FULL_MASK
        Assertions.assertEquals("ab****yz", MaskUtils.mask("abcdefghijxyz", 2, 2, null));
    }

    @Test
    void testMaskNullInput() {
        Assertions.assertNull(MaskUtils.mask(null, 3, 4));
        Assertions.assertNull(MaskUtils.mask(null, 3, 4, "----"));
    }

    @Test
    void testMaskTooShort() {
        // keepPrefix + keepSuffix >= length，整体脱敏
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.mask("123", 2, 2));
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.mask("1234", 2, 2));
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.mask("", 2, 2));
    }

    @Test
    void testMaskNegativeKeep() {
        // 负数按 0 处理
        Assertions.assertEquals("****yz", MaskUtils.mask("abcdefghijxyz", -1, 2));
        Assertions.assertEquals("abc****", MaskUtils.mask("abcdefghijxyz", 3, -2));
    }

    // ==================== maskByLength（按原长填星） ====================

    @Test
    void testMaskByLength() {
        // 中间星号数 = 被掩码字符数
        Assertions.assertEquals("138****5678", MaskUtils.maskByLength("13812345678", 3, 4));
        Assertions.assertEquals("ab*********yz", MaskUtils.maskByLength("abcdefghijxyz", 2, 2));
        Assertions.assertNull(MaskUtils.maskByLength(null, 1, 1));
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.maskByLength("123", 2, 2));
    }

    // ==================== secret（固定掩码，凭证类） ====================

    @Test
    void testSecret() {
        // 固定掩码，不随长度变化
        Assertions.assertEquals("abcd****wxyz", MaskUtils.maskSecret("abcdefghijklmnopwxyz"));
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.maskSecret("short"));
        Assertions.assertNull(MaskUtils.maskSecret(null));
    }

    // ==================== chinaMobile ====================

    @Test
    void testChinaMobile() {
        Assertions.assertEquals("138****5678", MaskUtils.maskChinaMobile("13812345678"));
        Assertions.assertNull(MaskUtils.maskChinaMobile(null));
    }

    // ==================== telephone ====================

    @Test
    void testTelephone() {
        Assertions.assertEquals("0101*****78", MaskUtils.maskTelephone("01012345678"));
        Assertions.assertNull(MaskUtils.maskTelephone(null));
    }

    // ==================== chinaIdCard ====================

    @Test
    void testChinaIdCard() {
        Assertions.assertEquals("110101********1234", MaskUtils.maskChinaIdCard("110101199001011234"));
        Assertions.assertNull(MaskUtils.maskChinaIdCard(null));
    }

    // ==================== passport ====================

    @Test
    void testPassport() {
        Assertions.assertEquals("E1*****78", MaskUtils.maskPassport("E12345678"));
        Assertions.assertNull(MaskUtils.maskPassport(null));
    }

    // ==================== chinaName ====================

    @Test
    void testChinaName() {
        Assertions.assertEquals("张*", MaskUtils.maskChinaName("张三"));
        Assertions.assertEquals("欧**", MaskUtils.maskChinaName("欧阳修"));
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.maskChinaName("张"));
        Assertions.assertNull(MaskUtils.maskChinaName(null));
    }

    // ==================== bankCard ====================

    @Test
    void testBankCard() {
        Assertions.assertEquals("6222********7890", MaskUtils.maskBankCard("6222021234567890"));
        Assertions.assertNull(MaskUtils.maskBankCard(null));
    }

    // ==================== email ====================

    @Test
    void testEmail() {
        Assertions.assertEquals("a****@example.com", MaskUtils.maskEmail("alice@example.com"));
        // 本地部分保留首字符，其余填星
        Assertions.assertEquals("a*@x.com", MaskUtils.maskEmail("ab@x.com"));
        Assertions.assertNull(MaskUtils.maskEmail(null));
        // 无 @
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.maskEmail("noatsign"));
    }

    @Test
    void testEmailLocalPartSingleChar() {
        // 本地部分仅 1 字符，保留该字符
        Assertions.assertEquals("a@x.com", MaskUtils.maskEmail("a@x.com"));
    }

    // ==================== chinaPlateNo ====================

    @Test
    void testChinaPlateNo() {
        Assertions.assertEquals("京A*****", MaskUtils.maskChinaPlateNo("京A12345"));
        // 新能源8位：前2位（京A）保留，其余6位填星
        Assertions.assertEquals("京A******", MaskUtils.maskChinaPlateNo("京AD12345"));
        Assertions.assertNull(MaskUtils.maskChinaPlateNo(null));
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.maskChinaPlateNo("京"));
    }

    // ==================== chinaUscc ====================

    @Test
    void testChinaUscc() {
        // 18 位统一社会信用代码示例
        Assertions.assertEquals("9111**********1234", MaskUtils.maskChinaUscc("911101081234561234"));
        Assertions.assertNull(MaskUtils.maskChinaUscc(null));
    }

    // ==================== chinaTaxNo ====================

    @Test
    void testChinaTaxNo() {
        // 18 位税号（与 USCC 同形）
        Assertions.assertEquals("9111**********1234", MaskUtils.maskChinaTaxNo("911101081234561234"));
        // 20 位税号兼容（中间 12 位填星）
        Assertions.assertEquals("1234************7890", MaskUtils.maskChinaTaxNo("12345678901234567890"));
        Assertions.assertNull(MaskUtils.maskChinaTaxNo(null));
    }

    // ==================== address ====================

    @Test
    void testAddress() {
        // 17 字，保留前6（北京市海淀区），其余11位填星
        Assertions.assertEquals("北京市海淀区***********", MaskUtils.maskAddress("北京市海淀区中关村大街1号院2号楼"));
        Assertions.assertNull(MaskUtils.maskAddress(null));
    }

    // ==================== imei ====================

    @Test
    void testImei() {
        // 15 位 IMEI，保留前6后2，中间7位填星
        Assertions.assertEquals("490154*******18", MaskUtils.maskImei("490154203237518"));
        Assertions.assertNull(MaskUtils.maskImei(null));
    }

    // ==================== wechatId ====================

    @Test
    void testWechatId() {
        Assertions.assertEquals("a******x", MaskUtils.maskWechatId("alice_wx"));
        Assertions.assertEquals("t*m", MaskUtils.maskWechatId("tom"));
        // 长度 ≤ 2 整体脱敏
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.maskWechatId("ab"));
        Assertions.assertNull(MaskUtils.maskWechatId(null));
    }

    // ==================== ipv4 ====================

    @Test
    void testIpv4() {
        Assertions.assertEquals("192.168.*.*", MaskUtils.maskIpv4("192.168.1.100"));
        Assertions.assertNull(MaskUtils.maskIpv4(null));
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.maskIpv4("not-an-ip"));
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.maskIpv4("192.168.1"));
        Assertions.assertEquals(MaskUtils.FULL_MASK, MaskUtils.maskIpv4("192.168.1.abc"));
    }
}
