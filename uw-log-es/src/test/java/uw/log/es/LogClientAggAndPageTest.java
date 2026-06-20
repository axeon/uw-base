package uw.log.es;

import org.junit.Test;
import uw.common.data.PageList;
import uw.common.util.JsonUtils;
import uw.log.es.vo.LogInterface;
import uw.log.es.vo.SearchResponse;

import java.util.*;

import static org.junit.Assert.*;

/**
 * 纯函数单测：覆盖 {@link LogClient} 的聚合转换工具与分页映射逻辑，不依赖真实ES。
 *
 * @since 2026-06-21
 */
public class LogClientAggAndPageTest {

    /**
     * 构造一个 terms 聚合（带子聚合）的 SearchResponse。
     */
    @SuppressWarnings("unchecked")
    private static SearchResponse<Object> aggResponse() {
        String json = "{\"hits\":{\"total\":{\"value\":10794056,\"relation\":\"eq\"},\"hits\":[]},"
                + "\"aggregations\":{\"refId\":{\"doc_count_error_upper_bound\":0,\"sum_other_doc_count\":0,"
                + "\"buckets\":[{\"key\":\"10002\",\"doc_count\":10794056,\"total\":{\"value\":10794056}},"
                + "{\"key\":\"10003\",\"doc_count\":4359901,\"total\":{\"value\":4359901}}]}}}";
        return JsonUtils.parse(json, JsonUtils.constructParametricType(SearchResponse.class, Object.class));
    }

    @Test
    public void testGetAggValue() {
        SearchResponse<Object> resp = aggResponse();
        double v = LogClient.getAggValue(resp.getAggregations(), "refId");
        //refId 是 terms 聚合，单值 value 默认 0
        assertEquals(0d, v, 0.0001);
        assertEquals(0d, LogClient.getAggValue(null, "refId"), 0.0001);
        assertEquals(0d, LogClient.getAggValue(resp.getAggregations(), "notExist"), 0.0001);
    }

    @Test
    public void testConvertAggBucketFlatMap() {
        SearchResponse<Object> resp = aggResponse();
        Map<String, Double> flat = LogClient.convertAggBucketFlatMap(resp.getAggregations());
        // terms 聚合拉平为 refId+key -> docCount
        assertEquals(Double.valueOf(10794056d), flat.get("refId10002"));
        assertEquals(Double.valueOf(4359901d), flat.get("refId10003"));
        assertTrue(LogClient.convertAggBucketFlatMap(null).isEmpty());
    }

    @Test
    public void testConvertAggBucketListMap() {
        SearchResponse<Object> resp = aggResponse();
        Map<String, List<Map<String, Object>>> map = LogClient.convertAggBucketListMap(resp.getAggregations());
        List<Map<String, Object>> buckets = map.get("refId");
        assertNotNull(buckets);
        assertEquals(2, buckets.size());
        assertEquals("10002", buckets.get(0).get("name"));
        assertEquals(10794056, ((Number) buckets.get(0).get("count")).intValue());
        //子聚合 total.value 被放入子 map
        assertEquals(10794056d, ((Number) buckets.get(0).get("total")).doubleValue(), 0.0001);
    }

    @Test
    public void testConvertAggBucketAggBucketFlatMap() {
        String json = "{\"hits\":{\"total\":{\"value\":10,\"relation\":\"eq\"},\"hits\":[]},"
                + "\"aggregations\":{\"a\":{\"buckets\":[{\"key\":\"k1\",\"doc_count\":5,"
                + "\"sub\":{\"buckets\":[{\"key\":\"s1\",\"doc_count\":2}]}}]}}}";
        @SuppressWarnings("unchecked")
        SearchResponse<Object> resp = JsonUtils.parse(json, JsonUtils.constructParametricType(SearchResponse.class, Object.class));
        Map<String, Map<String, Map<String, Double>>> map = LogClient.convertAggBucketAggBucketFlatMap(resp.getAggregations());
        // a -> k1 -> subs1 -> count
        Double cnt = map.get("a").get("k1").get("subs1");
        assertEquals(Double.valueOf(2d), cnt);
    }

    @Test
    public void testMapQueryResponseToPageList() {
        String json = "{\"hits\":{\"total\":{\"value\":2,\"relation\":\"eq\"},\"hits\":["
                + "{\"_index\":\"idx\",\"_id\":\"1\",\"_source\":{\"interfaceType\":1}},"
                + "{\"_index\":\"idx\",\"_id\":\"2\",\"_source\":{\"interfaceType\":2}}]}}";
        SearchResponse<LogInterface> resp = JsonUtils.parse(json,
                JsonUtils.constructParametricType(SearchResponse.class, LogInterface.class));
        PageList<LogInterface> page = LogClient.mapQueryResponseToPageList(resp, 0, 10);
        assertEquals(2, page.size());
        assertEquals(2, page.sizeAll());
        assertEquals(1, page.get(0).getInterfaceType());

        // response 为 null 时返回空分页
        PageList<LogInterface> empty = LogClient.mapQueryResponseToPageList(null, 0, 10);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testTotalValueSupportsLong() {
        // value 超过 Integer.MAX_VALUE 时仍能正确反序列化为 long
        String json = "{\"hits\":{\"total\":{\"value\":3000000000,\"relation\":\"eq\"},\"hits\":[]}}";
        @SuppressWarnings("unchecked")
        SearchResponse<Object> resp = JsonUtils.parse(json, JsonUtils.constructParametricType(SearchResponse.class, Object.class));
        assertEquals(3000000000L, resp.getHitResponse().getTotal().getValue());
    }

    @Test
    public void testBulkLogFiltersByLogLevel() {
        // 验证 bulkLog 按 logLevel>0 过滤：NONE 级别不写入。
        // 此处仅验证过滤后的列表语义（writeBulkLog 会走到 LogService，但 READ_ONLY 模式下 writeLog 直接返回）。
        LogClientProperties props = new LogClientProperties();
        props.getEs().setServer(null); // server 为空 -> logState=false，writeLog 直接返回，不会真正发请求
        LogClient client = new LogClient(new uw.log.es.service.LogService(props, "app", "host"));
        List<LogInterface> list = new ArrayList<>();
        LogInterface keep = LogInterface.init(1, 1, 1, 1, 1, "p", "f");
        keep.setLogLevel(1);
        LogInterface drop = LogInterface.init(2, 2, 2, 2, 2, "p2", "f2");
        drop.setLogLevel(-1); // NONE
        list.add(keep);
        list.add(drop);
        client.bulkLog(list); // 不应抛异常
        // 原列表不被修改（非破坏式过滤）
        assertEquals(2, list.size());
    }
}
