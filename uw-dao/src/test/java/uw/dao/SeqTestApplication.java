package uw.dao;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sequence 测试专用启动类。
 *
 * <p>配合 {@code application-seqtest.yml} 使用，启动后 DaoSequenceFactory 与 FusionSequenceFactory
 * 均自动就绪。测试通过 {@code @ActiveProfiles("seqtest")} 激活对应配置。</p>
 *
 * @author axeon
 */
@SpringBootApplication
public class SeqTestApplication {
}
