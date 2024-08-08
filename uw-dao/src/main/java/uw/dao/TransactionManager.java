package uw.dao;

import java.sql.Connection;

/**
 * 事务操作管理类.
 */

public interface TransactionManager {

    /**
     * 事务级别 0.
     */
    int TRANSACTION_NONE = Connection.TRANSACTION_NONE;
    /**
     * 事务级别 2.
     */
    int TRANSACTION_READ_COMMITTED = Connection.TRANSACTION_READ_COMMITTED;
    /**
     * 事务级别 1.
     */
    int TRANSACTION_READ_UNCOMMITTED = Connection.TRANSACTION_READ_UNCOMMITTED;
    /**
     * 事务级别 4.
     */
    int TRANSACTION_REPEATABLE_READ = Connection.TRANSACTION_REPEATABLE_READ;
    /**
     * 事务级别 8.
     */
    int TRANSACTION_SERIALIZABLE = Connection.TRANSACTION_SERIALIZABLE;

    /**
     * 是否自动提交事务.
     *
     * @return boolean
     */
    public boolean isAutoCommit();

    /**
     * 提交该事务.
     *
     * @throws TransactionException 事务异常
     */
    public void commit() throws TransactionException;

    /**
     * 回滚该事务.
     *
     * @throws TransactionException 事务异常
     */
    public void rollback() throws TransactionException;

    /**
     * 获取当前事务级别.
     *
     * @return 事务级别
     * @throws TransactionException 事务异常
     */
    public int getTransactionIsolation();

    /**
     * 设置事务级别.
     *
     * @param level 级别
     * @throws TransactionException 事务异常
     */
    public void setTransactionIsolation(int level);

}
