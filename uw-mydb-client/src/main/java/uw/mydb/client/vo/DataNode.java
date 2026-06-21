package uw.mydb.client.vo;

/**
 * 数据节点对象。
 * <p>
 * 数据节点通过 MySQL 集群 ID 与库名的组合来确定唯一性，是分库分表路由的最小单元。
 * 一个 {@code DataNode} 形如 {@code "clusterId.database"}，可通过 {@link #toString()}
 * 序列化为该字符串，也可通过 {@link #DataNode(String)} 从该字符串反序列化。
 *
 * @author axeon
 */
public class DataNode {

    /**
     * MySQL 集群 ID。
     */
    private long clusterId;

    /**
     * MySQL 库名。
     */
    private String database;

    /**
     * 默认构造方法（用于反序列化）。
     */
    public DataNode() {
    }

    /**
     * 通过集群 ID 与库名构造数据节点。
     *
     * @param clusterId MySQL 集群 ID
     * @param database  MySQL 库名
     */
    public DataNode(long clusterId, String database) {
        this.clusterId = clusterId;
        this.database = database;
    }

    /**
     * 通过 {@code "clusterId.database"} 格式的字符串构造数据节点。
     * <p>
     * 对入参做格式与合法性校验：
     * <ul>
     *   <li>不允许为 {@code null} 或空串；</li>
     *   <li>必须包含且仅以第一个 {@code '.'} 作为分隔符，分隔符前后均不可为空；</li>
     *   <li>{@code clusterId} 部分必须为合法的 long 数值。</li>
     * </ul>
     * 任一校验失败均抛出 {@link IllegalArgumentException}。
     *
     * @param dataNodeKey 形如 {@code "clusterId.database"} 的节点标识
     * @throws IllegalArgumentException dataNodeKey 格式不合法时抛出
     */
    public DataNode(String dataNodeKey) {
        if (dataNodeKey == null || dataNodeKey.isEmpty()) {
            throw new IllegalArgumentException("dataNodeKey must not be null or empty");
        }
        int splitPos = dataNodeKey.indexOf('.');
        if (splitPos <= 0 || splitPos == dataNodeKey.length() - 1) {
            throw new IllegalArgumentException("dataNodeKey format invalid, expected: clusterId.database, got: " + dataNodeKey);
        }
        try {
            clusterId = Long.parseLong(dataNodeKey.substring(0, splitPos));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("clusterId is not a valid number in dataNodeKey: " + dataNodeKey, e);
        }
        database = dataNodeKey.substring(splitPos + 1);
        if (database.isEmpty()) {
            throw new IllegalArgumentException("database must not be empty in dataNodeKey: " + dataNodeKey);
        }
    }


    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * 序列化为 {@code "clusterId.database"} 格式的字符串。
     *
     * @return 形如 {@code "clusterId.database"} 的节点标识
     */
    @Override
    public String toString() {
        return new StringBuilder().append(this.clusterId).append('.').append(database).toString();
    }
}
