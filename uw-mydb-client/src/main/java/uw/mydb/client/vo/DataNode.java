package uw.mydb.client.vo;

/**
 * 数据节点对象。
 * 数据节点通过服务器集群和库来确定唯一性。
 */
public class DataNode {

    /**
     * mysql集群。
     */
    private long clusterId;

    /**
     * mysql库名。
     */
    private String database;

    public DataNode() {
    }

    public DataNode(long clusterId, String database) {
        this.clusterId = clusterId;
        this.database = database;
    }

    /**
     * 通过集群ID.数据库名来确定唯一值。
     *
     * @param dataNodeKey
     * @throws IllegalArgumentException dataNodeKey格式不合法时抛出
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

    @Override
    public String toString() {
        return new StringBuilder().append(this.clusterId).append('.').append(database).toString();
    }
}
