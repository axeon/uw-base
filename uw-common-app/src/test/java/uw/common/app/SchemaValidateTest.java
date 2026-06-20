package uw.common.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uw.common.app.helper.SchemaValidateHelper;

import java.util.List;

/**
 * Schema 数据校验工具测试。
 */
public class SchemaValidateTest {

    @Test
    void testRequiredFieldsMissing() {
        SchemaTestVo vo = new SchemaTestVo();
        // 仅设置 id，必填字段 name/price/createDate/status 缺失，应返回对应校验失败结果
        vo.setId(1);
        List<?> results = SchemaValidateHelper.validate(vo);
        Assertions.assertFalse(results.isEmpty(), "缺少必填字段时应返回校验失败结果");
    }

    @Test
    void testAllRequiredPresent() {
        SchemaTestVo vo = new SchemaTestVo(
                1L, "name", null, 9.9,
                new java.util.Date(), null, 1
        );
        List<?> results = SchemaValidateHelper.validate(vo);
        Assertions.assertTrue(results.isEmpty(), "所有必填字段齐全且数值合法时应无校验失败");
    }
}
