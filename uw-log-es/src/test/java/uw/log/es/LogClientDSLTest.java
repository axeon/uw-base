package uw.log.es;

import org.junit.BeforeClass;
import org.junit.Test;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.log.es.service.LogService;
import uw.log.es.vo.*;


/**
 * 
 * @since 2018-07-27
 */
public class LogClientDSLTest {

    private static LogClient logClient;

    private static LogService logService;

    @BeforeClass
    public static void setUpTest() {
        LogClientProperties logClientProperties = new LogClientProperties();
        LogClientProperties.EsConfig esConfig = new LogClientProperties.EsConfig();
        esConfig.setServer("http://localhost:9200");
        esConfig.setMode(LogClientProperties.LogMode.READ_WRITE);
        esConfig.setAppInfoOverwrite(false);
        esConfig.setMaxFlushInSeconds(10);
        esConfig.setMaxKiloBytesOfBatch(5 * 1024);
        esConfig.setMaxBatchThreads(5);
        logClientProperties.setEs(esConfig);
        logService = new LogService(logClientProperties, null, null);
        logClient = new LogClient(logService);
        logClient.regLogObjectWithIndexPattern(LogInterface.class, "_yyyy-MM");
        logClient.regLogObjectWithIndexPattern(LogInterfaceOrder.class, "_yyyy-MM");
    }


    @Test
    public void testDslResultParse() {
        String result = "{\"took\":3,\"timed_out\":false,\"_shards\":{\"total\":75,\"successful\":75,\"skipped\":0,\"failed\":0},\"hits\":{\"total\":16862329,\"max_score\":0,\"hits\":[]},\"aggregations\":{\"refId\":{\"doc_count_error_upper_bound\":0,\"sum_other_doc_count\":0,\"buckets\":[{\"key\":10002,\"doc_count\":10794056,\"total\":{\"value\":10794056}},{\"key\":10003,\"doc_count\":4359901,\"total\":{\"value\":4359901}},{\"key\":0,\"doc_count\":1708372,\"total\":{\"value\":1708372}}]}}}";
        SearchResponse<Object> response = null;
        try {
            response = JsonInterfaceHelper.JSON_CONVERTER.parse(result,
                    JsonInterfaceHelper.JSON_CONVERTER
                            .constructParametricType(SearchResponse.class, Object.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(response);
    }

    @Test
    public void testSqlToDsl() throws Exception {

        logClient.regLogObjectWithIndexNameAndPattern(MscActionLog.class, "joy-party", "yyyyMMdd");
        String logIndex = logClient.getQuotedQueryIndexName(MscActionLog.class);

        System.out.println(logClient.translateSqlToDsl("SELECT count(*) from " + logIndex, 0, 0, true));
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
        String dsl = logService.translateSqlToDsl("select * from \\\"saas-hotel-task_20191217\\\"", 0, 10, true);
        ScrollResponse<TaskRunnerLog> taskRunnerLogScrollResponse = logClient.scrollQueryOpen(TaskRunnerLog.class, "uw.auth.server.vo.msc_action_log_20191217", 60, dsl);
        System.out.println(taskRunnerLogScrollResponse);
    }

    @Test
    public void testScrollNext() {
        ScrollResponse<TaskRunnerLog> taskRunnerLogScrollResponse = logClient.scrollQueryNext(TaskRunnerLog.class, null, "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAFqj0WbTg3Z180Q1FRUHEwelZjbUI0NmROQQ==", 60);
        System.out.println(taskRunnerLogScrollResponse);

    }

    @Test
    public void deleteScroll() {
        DeleteScrollResponse deleteScrollResponse = logClient.scrollQueryClose("DnF1ZXJ5VGhlbkZldGNoBAAAAAAABXUDFm04N2dfNENRUVBxMHpWY21CNDZkTkEAAAAAAAV1BBZtODdnXzRDUVFQcTB6VmNtQjQ2ZE5BAAAAAAAFdQUWbTg3Z180Q1FRUHEwelZjbUI0NmROQQAAAAAABXUZFm04N2dfNENRUVBxMHpWY21CNDZkTkE=",
                null);
        System.out.println(deleteScrollResponse);
    }

}
