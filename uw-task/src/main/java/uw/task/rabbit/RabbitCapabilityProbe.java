package uw.task.rabbit;

import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/**
 * RabbitMQ 能力探测：启动期一次性判定 Broker 版本、AMQP 1.0 监听是否开放、原生延迟消息是否可用。
 *
 * <p>决策模型（261120 定案，纯自动无配置）：1.0 可用则走 1.0 路径，否则 091；
 * 原生延迟（x-opt-delivery-time，4.4+ quorum）可用则延时任务走 MQ 消息级延迟，否则走 Redis zset 搬运。</p>
 *
 * <p>探测手段（全部经 amqp 端口本身，与管理端口解耦）：①1.0 协议头握手判定 1.0 可用性；
 * ②0-9-1 握手 connection.start 帧解析版本号。版本未知时保守关闭原生延迟（降级 redis zset），不阻断启动。</p>
 *
 * @author axeon
 */
public class RabbitCapabilityProbe {

    private static final Logger log = LoggerFactory.getLogger(RabbitCapabilityProbe.class);

    /**
     * 原生延迟消息要求的最低 RabbitMQ 版本（4.4 引入 x-opt-delivery-time，仅 quorum queue）。
     */
    private static final int[] NATIVE_DELAY_MIN_VERSION = {4, 4, 0};

    /**
     * 探测 TCP 连接超时（毫秒）。
     */
    private static final int PROBE_TIMEOUT_MS = 3000;

    /**
     * AMQP 1.0 是否可用（协议头握手判定）。
     */
    private final boolean amqp100Available;

    /**
     * RabbitMQ 版本号（0-9-1 connection.start 解析；未知为 null）。
     */
    private final String rabbitmqVersion;

    /**
     * 原生延迟消息是否可用（1.0 可用且版本 ≥4.4 且最终协议为 1.0）。
     */
    private final boolean nativeDelayAvailable;

    /**
     * 最终选定的协议（配置 auto 时按探测结果定）。
     */
    private final TaskAmqpProtocol protocol;

    private RabbitCapabilityProbe(boolean amqp100Available, String version, boolean nativeDelayAvailable) {
        this.amqp100Available = amqp100Available;
        // 原生延迟必须落在最终选定的 1.0 协议上（当前 auto-only：1.0 可用即选 1.0，恒成立，
        // 保留判定以防未来引入强制开关时回归）
        this.rabbitmqVersion = version;
        this.protocol = amqp100Available ? TaskAmqpProtocol.AMQP_100 : TaskAmqpProtocol.AMQP_091;
        this.nativeDelayAvailable = nativeDelayAvailable && this.protocol == TaskAmqpProtocol.AMQP_100;
    }

    /**
     * 执行探测：对全部解析端点（集群多节点逐一尝试，单节点即一个）做 1.0 协议头握手；
     * 首个握出 1.0 的节点再经 0-9-1 握手取版本号。
     *
     * @param rabbitProps rabbit 连接配置（uw.task.rabbitmq.*，与 spring.rabbitmq 同构）
     * @return 探测结果
     */
    public static RabbitCapabilityProbe probe(RabbitProperties rabbitProps) {
        // 集群支持：uw.task.rabbitmq.addresses 多节点时逐个探测，首个可用节点的结果即集群能力
        // （集群内节点版本/协议能力一致；addresses 未配置时退回单 host:port）
        // TaskAmqpOps.resolveEndpoints()：未配 addresses 时返回单元素，天然覆盖两种形态
        List<TaskAmqpOps.AmqpEndpoint> endpoints = TaskAmqpOps.resolveEndpoints(rabbitProps);
        boolean amqp10Listener = false;
        String version = null;
        for (TaskAmqpOps.AmqpEndpoint endpoint : endpoints) {
            String host = endpoint.host();
            int amqpPort = endpoint.port();
            // ① AMQP 1.0 协议头握手：对端回 major=1 即支持 1.0（4.x 与 0-9-1 同端口协商，无独立 5673）
            amqp10Listener = amqp10HandshakeProbe(host, amqpPort);
            if (amqp10Listener) {
                // ② 版本号经 0-9-1 握手获取：connection.start 帧 server-properties 携带 product/version
                version = amqp091VersionProbe(host, amqpPort);
                break;
            }
        }
        boolean nativeDelay = amqp10Listener && versionAtLeast(version, NATIVE_DELAY_MIN_VERSION);
        RabbitCapabilityProbe result = new RabbitCapabilityProbe(amqp10Listener, version, nativeDelay);
        log.info("RabbitCapabilityProbe 判定: rabbitmqVersion={}, amqp1.0={}, nativeDelay={}, protocol={}, delayImpl={}",
                version == null ? "unknown" : version, amqp10Listener, nativeDelay, result.getProtocol(),
                result.isNativeDelayAvailable() ? "mq" : "redis");
        return result;
    }

    /**
     * 0-9-1 握手取版本号：发送 0-9-1 协议头（AMQP\x00\x00\x09\x01），读取 connection.start 帧，
     * 解析 server-properties field table 的 version 字段。失败返回 null（版本未知→原生延迟保守关闭）。
     */
    private static String amqp091VersionProbe(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(PROBE_TIMEOUT_MS);
            socket.connect(new InetSocketAddress(host, port), PROBE_TIMEOUT_MS);
            socket.getOutputStream().write(new byte[]{'A', 'M', 'Q', 'P', 0, 0, 9, 1});
            socket.getOutputStream().flush();
            // 帧头：type(1) + channel(2) + length(4)，后跟 frame body
            DataInputStream in = new DataInputStream(socket.getInputStream());
            int frameType = in.readUnsignedByte();
            in.readUnsignedShort();
            long frameLength = in.readInt() & 0xFFFFFFFFL;
            if (frameType != 1 || frameLength <= 0 || frameLength > 8192) {
                return null;
            }
            byte[] frame = new byte[(int) frameLength];
            in.readFully(frame);
            // connection.start body: version-major(1) version-minor(1) server-properties(field table) ...
            // field table: length(4) + entries；在帧字节里定位 "version" 字段（短字符串键 + 长字符串值）
            String version = parseServerProperty(frame, "version");
            String product = parseServerProperty(frame, "product");
            if (product != null && !product.contains("RabbitMQ")) {
                // 非 RabbitMQ（如阿里云 AMQP 版兼容变体）：版本号语义不可信，视为未知
                log.warn("RabbitCapabilityProbe: 对端 product={} 非 RabbitMQ，版本判级跳过", product);
                return null;
            }
            return version;
        } catch (Exception e) {
            log.warn("RabbitCapabilityProbe: 0-9-1 版本探测失败({}:{}): {}", host, port, e.getMessage());
            return null;
        }
    }

    /**
     * 在 connection.start 帧字节中朴素定位 field table 字段：短字符串键 "version"/"product" 后跟 'S'(长字符串) 值。
     */
    private static String parseServerProperty(byte[] frame, String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < frame.length - keyBytes.length - 2; i++) {
            if (frame[i] == keyBytes.length && Arrays.equals(frame, i + 1, i + 1 + keyBytes.length, keyBytes, 0, keyBytes.length)) {
                int v = i + 1 + keyBytes.length;
                if (v < frame.length && frame[v] == 'S') {
                    int len = ((frame[v + 1] & 0xFF) << 24) | ((frame[v + 2] & 0xFF) << 16) | ((frame[v + 3] & 0xFF) << 8) | (frame[v + 4] & 0xFF);
                    if (len > 0 && v + 5 + len <= frame.length) {
                        return new String(frame, v + 5, len, StandardCharsets.US_ASCII);
                    }
                }
            }
        }
        return null;
    }

    /**
     * AMQP 1.0 协议头握手探测：发送版本协商头 AMQP\x00\x01\x00\x00（protocol-id=0 AMQP），
     * 期望对端回应 AMQP 1.0 协议头（"AMQP" + 00 01 00 00）。
     * 半开端口（安全组代答）会在读回包时 EOF/超时，探测为否。
     */
    private static boolean amqp10HandshakeProbe(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(PROBE_TIMEOUT_MS);
            socket.connect(new InetSocketAddress(host, port), PROBE_TIMEOUT_MS);
            socket.getOutputStream().write(new byte[]{'A', 'M', 'Q', 'P', 0, 1, 0, 0});
            socket.getOutputStream().flush();
            byte[] reply = new byte[8];
            int read = 0;
            while (read < 8) {
                int n = socket.getInputStream().read(reply, read, 8 - read);
                if (n < 0) {
                    return false;
                }
                read += n;
            }
            // 对端回 "AMQP" + protoId + major.minor.revision：1.0 回包 major=1（实测 4.4.1 回 03 01 00 00，
            // protoId=3）；0-9-1-only 回包 major=0 minor=9（00 00 09 01）后断开
            return reply[0] == 'A' && reply[1] == 'M' && reply[2] == 'Q' && reply[3] == 'P'
                    && reply[5] == 1 && reply[6] == 0 && reply[7] == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 版本比较：version ≥ min[0].[1].[2]。解析失败视为不满足（保守降级）。
     */
    static boolean versionAtLeast(String version, int[] min) {
        if (version == null || version.isBlank()) {
            return false;
        }
        String[] parts = version.split("[^0-9]");
        for (int i = 0; i < min.length; i++) {
            int v = 0;
            if (i < parts.length && !parts[i].isEmpty()) {
                try {
                    v = Integer.parseInt(parts[i]);
                } catch (NumberFormatException ignore) {
                    return false;
                }
            }
            if (v > min[i]) {
                return true;
            }
            if (v < min[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return AMQP 1.0 是否可用（协议头握手判定，与最终选定协议无关）
     */
    public boolean isAmqp100Available() {
        return amqp100Available;
    }

    /**
     * @return RabbitMQ 版本号；探测失败/非 RabbitMQ 对端返回 null
     */
    public String getRabbitmqVersion() {
        return rabbitmqVersion;
    }

    /**
     * 原生延迟消息可用：1.0 可用且 RabbitMQ ≥ 4.4。延时任务（taskDelay&gt;0 且 quorum 队列）走 MQ
     * 消息级延迟（x-opt-delivery-time），否则走 Redis zset 搬运（TaskDelayTransferContainer）。
     */
    public boolean isNativeDelayAvailable() {
        return nativeDelayAvailable;
    }

    /**
     * 最终选定的协议。
     */
    public TaskAmqpProtocol getProtocol() {
        return protocol;
    }
}
