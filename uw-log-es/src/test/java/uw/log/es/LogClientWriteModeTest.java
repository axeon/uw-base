package uw.log.es;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uw.common.util.SystemClock;
import uw.log.es.service.LogService;
import uw.log.es.vo.LogInterface;
import uw.log.es.vo.LogInterfaceOrder;


/**
 *
 * @since 2018-07-27
 */
public class LogClientWriteModeTest {

    private static LogClient logClient;

    @BeforeClass
    public static void setUpTest() {
        UwLogEsProperties logClientProperties = new UwLogEsProperties();
        UwLogEsProperties.EsConfig esConfig = new UwLogEsProperties.EsConfig();
        esConfig.setServer("http://localhost:9200");
        esConfig.setMode(UwLogEsProperties.LogMode.READ_WRITE);
        esConfig.setAppInfoOverwrite(false);
        esConfig.setMaxFlushInSeconds(10);
        esConfig.setMaxKiloBytesOfBatch(5 * 1024);
        esConfig.setMaxBatchThreads(5);
        logClientProperties.setEs(esConfig);
        logClient = new LogClient(new LogService(logClientProperties, null, null));
        logClient.regLogObjectWithIndexPattern(LogInterface.class, "yyyyMM");
        logClient.regLogObjectWithIndexPattern(LogInterfaceOrder.class, "yyyyMM");
    }

    @Test
    public void testWriteLog() throws Exception {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            LogInterface logInterface = new LogInterface();
            logInterface.setInterfaceType(1);
            logInterface.setInterfaceConfigId(Long.parseLong(RandomStringUtils.secure().nextNumeric(6)));
            logInterface.setSaasId(Long.parseLong(RandomStringUtils.secure().nextNumeric(6)));
            logInterface.setProductType(10);
            logInterface.setProductId(Long.parseLong(RandomStringUtils.secure().nextNumeric(6)));
            logInterface.setInterfaceProductId(RandomStringUtils.secure().nextNumeric(11));
            logInterface.setInterfaceFunction("zwy.common.log.client.logInterface");
            logInterface.setRequestDate(SystemClock.nowDate());
            logInterface.setRequestBody("你吃饭了吗?");
            logInterface.setResponseDate(SystemClock.nowDate());
            logInterface.setResponseBody("吃了");
            logClient.log(logInterface);
        }
        System.out.println("----" + (System.currentTimeMillis() - startTime));
    }


    @AfterClass
    public static void tearDownTest() {
        if (logClient != null) {
            logClient.destroy();
        }
    }
}
