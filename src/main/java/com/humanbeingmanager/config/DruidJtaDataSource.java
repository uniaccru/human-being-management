package com.humanbeingmanager.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Обертка Druid DataSource для поддержки JTA транзакций в WildFly
 * 
 * Этот класс позволяет использовать Druid DataSource в JTA окружении,
 * оборачивая его для совместимости с WildFly transaction manager.
 */
public class DruidJtaDataSource implements DataSource, XADataSource {
    
    private final DruidDataSource druidDataSource;
    
    public DruidJtaDataSource(DruidDataSource druidDataSource) {
        this.druidDataSource = druidDataSource;
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return druidDataSource.getConnection();
    }
    
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return druidDataSource.getConnection(username, password);
    }
    
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return druidDataSource.getLogWriter();
    }
    
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        druidDataSource.setLogWriter(out);
    }
    
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        druidDataSource.setLoginTimeout(seconds);
    }
    
    @Override
    public int getLoginTimeout() throws SQLException {
        return druidDataSource.getLoginTimeout();
    }
    
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return druidDataSource.getParentLogger();
    }
    
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return druidDataSource.unwrap(iface);
    }
    
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || druidDataSource.isWrapperFor(iface);
    }
    
    // XADataSource методы - для поддержки распределенных транзакций
    @Override
    public XAConnection getXAConnection() throws SQLException {
        // Druid не поддерживает XA напрямую, но мы можем обернуть обычное соединение
        Connection conn = druidDataSource.getConnection();
        return new DruidXAConnection(conn);
    }
    
    @Override
    public XAConnection getXAConnection(String user, String password) throws SQLException {
        Connection conn = druidDataSource.getConnection(user, password);
        return new DruidXAConnection(conn);
    }
    
    /**
     * Простая обертка XAConnection для Druid
     */
    private static class DruidXAConnection implements XAConnection {
        private final Connection connection;
        
        public DruidXAConnection(Connection connection) {
            this.connection = connection;
        }
        
        @Override
        public XAResource getXAResource() throws SQLException {
            // Для простых транзакций возвращаем null
            // В реальном XA окружении нужна более сложная реализация
            return null;
        }
        
        @Override
        public Connection getConnection() throws SQLException {
            return connection;
        }
        
        @Override
        public void close() throws SQLException {
            connection.close();
        }
        
        @Override
        public void addConnectionEventListener(javax.sql.ConnectionEventListener listener) {
            // Не поддерживается
        }
        
        @Override
        public void removeConnectionEventListener(javax.sql.ConnectionEventListener listener) {
            // Не поддерживается
        }
        
        @Override
        public void addStatementEventListener(javax.sql.StatementEventListener listener) {
            // Не поддерживается
        }
        
        @Override
        public void removeStatementEventListener(javax.sql.StatementEventListener listener) {
            // Не поддерживается
        }
    }
}





