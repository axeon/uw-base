package uw.common.app;

import uw.common.app.helper.SchemaValidateHelper;

public class SchemaValidateTest {

    public static void main(String[] args) {
        SchemaTestVo vo = new SchemaTestVo();
        vo.setId(1);
        System.out.println(SchemaValidateHelper.validate(vo));
    }

}
