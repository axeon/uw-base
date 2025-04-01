package uw.log.es;

import uw.common.util.JsonUtils;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.log.es.vo.ScrollResponse;
import uw.log.es.vo.TaskRunnerLog;
import uw.httpclient.exception.DataMapperException;

/**
 * 
 * @since 2019-12-16
 **/
public class VoTest {

    public static void main(String[] args) throws DataMapperException {
        String a = "{\n" +
                "  \"_scroll_id\": \"DnF1ZXJ5VGhlbkZldGNoAwAAAAAAA9nBFm04N2dfNENRUVBxMHpWY21CNDZkTkEAAAAAAAPZwhZtODdnXzRDUVFQcTB6VmNtQjQ2ZE5BAAAAAAAD2cQWbTg3Z180Q1FRUHEwelZjbUI0NmROQQ==\",\n" +
                "  \"took\": 4529,\n" +
                "  \"timed_out\": false,\n" +
                "  \"_shards\": {\n" +
                "    \"total\": 3,\n" +
                "    \"successful\": 3,\n" +
                "    \"skipped\": 0,\n" +
                "    \"failed\": 0\n" +
                "  },\n" +
                "  \"hits\": {\n" +
                "    \"total\": {\n" +
                "      \"value\": 10793698,\n" +
                "      \"relation\": \"eq\"\n" +
                "    },\n" +
                "    \"max_score\": null,\n" +
                "    \"hits\": [\n" +
                "      {\n" +
                "        \"_index\": \"uw.task.entity.task_runner_log_20191216\",\n" +
                "        \"_type\": \"logs\",\n" +
                "        \"_id\": \"ectKCm8Bhmtx6Op4LCnW\",\n" +
                "        \"_score\": null,\n" +
                "        \"_source\": {\n" +
                "          \"refTag\": \"2103010001\",\n" +
                "          \"runDate\": 1576425614220,\n" +
                "          \"resultData\": \"\",\n" +
                "          \"runType\": 3,\n" +
                "          \"errorInfo\": null,\n" +
                "          \"taskClass\": \"zwy.saas.ticket.task.supplier.lvmama.runner.ProductListPriceGetRunner\",\n" +
                "          \"taskTag\": \"10001\",\n" +
                "          \"consumeDate\": 1576425614220,\n" +
                "          \"appHost\": \"10.8.8.89/1@896JTP2.fs.zowoyoo.com\",\n" +
                "          \"runTarget\": \"\",\n" +
                "          \"rateLimitTag\": \"10349\",\n" +
                "          \"logLevel\": 0,\n" +
                "          \"queueDate\": 1576425600088,\n" +
                "          \"refSubId\": 10034,\n" +
                "          \"id\": 2620176181,\n" +
                "          \"state\": 1,\n" +
                "          \"hostIp\": \"10.8.8.89\",\n" +
                "          \"appName\": \"saas-ticket-task/1.1.67\",\n" +
                "          \"hostId\": \"1@896JTP2.fs.zowoyoo.com\",\n" +
                "          \"ranTimes\": 1,\n" +
                "          \"finishDate\": 1576425620173,\n" +
                "          \"refId\": 10001,\n" +
                "          \"taskDelay\": 0,\n" +
                "          \"retryType\": 0\n" +
                "        },\n" +
                "        \"fields\": {\n" +
                "          \"@timestamp\": [\n" +
                "            \"1576425621093\"\n" +
                "          ]\n" +
                "        },\n" +
                "        \"sort\": [\n" +
                "          0\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        ScrollResponse<TaskRunnerLog> scrollResponse = JsonUtils.parse(a,
                JsonUtils.constructParametricType(ScrollResponse.class, TaskRunnerLog.class));
        System.out.println(scrollResponse);
    }
}
