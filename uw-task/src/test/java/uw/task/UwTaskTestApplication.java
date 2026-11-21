package uw.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * uw-task 集成测试启动类（扮演"业务方应用"）。
 * <p>
 * 扫描 {@code uw.task} 包：测试 Runner（如 {@link uw.task.demo.EchoRunner}）被注册为 Bean，
 * 由 {@link uw.task.conf.UwTaskAutoConfiguration}（通过 META-INF/spring AutoConfiguration.imports 自动装配）
 * 扫描并启动监听容器、限速器、发号器、TaskFactory 等。
 * </p>
 * <p>
 * 测试期间连真实 RabbitMQ / Redis / task-center（见 src/test/resources/application.yml）。
 * </p>
 *
 * @author axeon
 */
@SpringBootApplication(scanBasePackages = "uw.task")
public class UwTaskTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(UwTaskTestApplication.class, args);
    }
}
