package uw.ai.test.tool;

import uw.ai.util.AiToolSchemaGenerator;

public class ToolSchemaTest {


    public static void main(String[] args) {
        System.out.println( AiToolSchemaGenerator.generateForMethodInput(NowDateTool.class.getMethods()[0]));
        System.out.println( AiToolSchemaGenerator.generateForMethodOutput(NowDateTool.class.getMethods()[0]));
    }

}
