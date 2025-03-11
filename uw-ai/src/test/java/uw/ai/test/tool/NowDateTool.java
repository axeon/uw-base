package uw.ai.test.tool;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.ai.tool.AiToolParam;
import uw.common.dto.ResponseData;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class NowDateTool implements uw.ai.tool.AiTool<NowDateTool.ToolParam, ResponseData<String>> {


    /**
     * 定义工具名称。
     *
     * @return
     */
    @Override
    public String toolName() {
        return "当前时间工具";
    }

    /**
     * 定义工具描述。
     *
     * @return
     */
    @Override
    public String toolDesc() {
        return "当前时间工具";
    }

    /**
     * 定义工具版本。
     *
     * @return
     */
    @Override
    public String toolVersion() {
        return "0.0.1";
    }

    /**
     * Applies this function to the given argument.
     *
     * @param toolParam the function argument
     * @return the function result
     */
    @Override
    public ResponseData<String> apply(ToolParam toolParam) {
        return ResponseData.success( LocalDateTime.now().toString() );
    }

    /**
     * 工具参数。
     */
    public static class ToolParam extends AiToolParam {

        @Schema(description = "时区，默认为UTC", requiredMode = Schema.RequiredMode.REQUIRED)
        private String timeZone = ZoneId.systemDefault().getId();

        public String getTimeZone() {
            return timeZone;
        }

        public void setTimeZone(String timeZone) {
            this.timeZone = timeZone;
        }

    }

}
