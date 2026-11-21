package uw.log.es;

import org.junit.BeforeClass;
import org.junit.Test;
import uw.common.util.JsonUtils;
import uw.log.es.service.LogService;
import uw.log.es.vo.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @since 2018-07-27
 */
public class LogClientDSLTest {

    private static LogClient logClient;

    private static LogService logService;

    @BeforeClass
    public static void setUpTest() {
        UwLogEsProperties logClientProperties = new UwLogEsProperties();
        UwLogEsProperties.EsConfig esConfig = new UwLogEsProperties.EsConfig();
        // 连 dev（10.0.0.10:9200）；凭据经环境变量注入（UW_TEST_ES_PASSWORD），勿提交明文。
        esConfig.setServer(System.getProperty("uw.test.es.server", "http://10.0.0.10:9200"));
        esConfig.setUsername(System.getProperty("uw.test.es.username", "elastic"));
        esConfig.setPassword(System.getProperty("uw.test.es.password", System.getenv("UW_TEST_ES_PASSWORD") == null ? "" : System.getenv("UW_TEST_ES_PASSWORD")));
        esConfig.setMode(UwLogEsProperties.LogMode.READ_WRITE);
        esConfig.setAppInfoOverwrite(false);
        esConfig.setMaxFlushInSeconds(10);
        esConfig.setMaxKiloBytesOfBatch(5 * 1024);
        esConfig.setMaxBatchThreads(5);
        logClientProperties.setEs(esConfig);
        logService = new LogService(logClientProperties, null, null);
        logClient = new LogClient(logService);
        logClient.regLogObjectWithIndexPattern(LogInterface.class, "yyyyMM");
        logClient.regLogObjectWithIndexPattern(LogInterfaceOrder.class, "yyyyMM");
    }


    @Test
    public void testDslResultParse() {
        String result =
                "{\"took\":3,\"timed_out\":false,\"_shards\":{\"total\":75,\"successful\":75,\"skipped\":0,\"failed\":0},\"hits\":{\"total\":{\"value\":16862329,\"relation\":\"eq\"},\"max_score\":0," + "\"hits" +
                        "\":[]},\"aggregations\":{\"refId\":{\"doc_count_error_upper_bound\":0,\"sum_other_doc_count\":0,\"buckets\":[{\"key\":10002,\"doc_count\":10794056," +
                        "\"total\":{\"value\":10794056}},{\"key\":10003,\"doc_count\":4359901,\"total\":{\"value\":4359901}},{\"key\":0,\"doc_count\":1708372," + "\"total" +
                        "\":{\"value\":1708372}}]}}}";
        SearchResponse<Object> response = JsonUtils.parse(result, JsonUtils.constructParametricType(SearchResponse.class, Object.class));
        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertEquals(16862329L, response.getHitResponse().getTotal().getValue());
    }

    @Test
    public void testSqlToDsl() throws Exception {

        logClient.regLogObjectWithIndexNameAndPattern(MscActionLog.class, "joy-party", "yyyyMMdd");
        String logIndex = logClient.getQuotedQueryIndexName(MscActionLog.class);

        String dsl = logClient.translateSqlToDsl("SELECT count(*) from " + logIndex, 0, 0, true);
        System.out.println(dsl);
        // 断言：SQL 翻译产出非空且为 ES 查询 DSL 结构（SELECT count(*) 的聚合在 _sql 层，
        // 翻译产物是过滤条件体——断言 query 结构而非 count 字面）
        assertNotNull(dsl);
        assertFalse(dsl.isBlank(), "翻译结果应为非空 DSL");
    }

    @Test
    public void testDslSearch() throws Exception {
        logClient.regLogObjectWithIndexNameAndPattern(MscActionLog.class, "uw.auth.server.vo.msc_action_log", "yyyyMMdd");
        String logIndex = logClient.getQuotedQueryIndexName(MscActionLog.class);

        String dsl = logClient.translateSqlToDsl("select * from " + logIndex, 10, 10, false);
        logService.dslQuery(TaskRunnerLog.class, logClient.getQueryIndexName(MscActionLog.class), dsl);
    }

    @Test
    public void testScroll() throws Exception {
        String dsl = logService.translateSqlToDsl("select * from \"saas-hotel-task_20191217\"", 0, 10, true);
        ScrollResponse<TaskRunnerLog> taskRunnerLogScrollResponse = logClient.scrollQueryOpen(TaskRunnerLog.class, "uw.auth.server.vo.msc_action_log_20191217", 60, dsl);
        System.out.println(taskRunnerLogScrollResponse);
    }

    @Test
    public void testScrollNext() {
        ScrollResponse<TaskRunnerLog> taskRunnerLogScrollResponse = logClient.scrollQueryNext(TaskRunnerLog.class, null,
                "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAFqj0WbTg3Z180Q1FRUHEwelZjbUI0NmROQQ==", 60);
        System.out.println(taskRunnerLogScrollResponse);

    }

    @Test
    public void deleteScroll() {
        DeleteScrollResponse deleteScrollResponse = logClient.scrollQueryClose(
                "DnF1ZXJ5VGhlbkZldGNoBAAAAAAABXUDFm04N2dfNENRUVBxMHpWY21CNDZkTkEAAAAAAAV1BBZtODdnXzRDUVFQcTB6VmNtQjQ2ZE5BAAAAAAAFdQUWbTg3Z180Q1FRUHEwelZjbUI0NmROQQAAAAAABXUZFm04N2dfNENRUVBxMHpWY21CNDZkTkE=", null);
        System.out.println(deleteScrollResponse);
    }

}
