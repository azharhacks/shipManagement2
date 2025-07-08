package com.shipmanagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the Cargo class that mock database interactions
 */
public class CargoDatabaseTest {
    
    // Use a test implementation instead of mocking Connection directly
    private TestDatabaseConnection testDbConnection;
    private PreparedStatement mockPreparedStatement;
    
    @BeforeEach
    void setUp() throws SQLException {
        // Create mocks manually instead of using annotations
        mockPreparedStatement = mock(PreparedStatement.class);
        testDbConnection = new TestDatabaseConnection(mockPreparedStatement);
        
        // Set up the mock behavior
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // Return success for executeUpdate
        
        // Set the test connection in DatabaseConnection
        DatabaseConnection.setTestConnection(testDbConnection);
    }
    
    @AfterEach
    void tearDown() {
        // Reset the test connection after each test
        DatabaseConnection.resetTestConnection();
    }
    
    @Test
    @DisplayName("Test saving cargo to database")
    void testSaveToDatabase() throws SQLException {
        // Create a cargo object which will call saveToDatabase() in its constructor
        Cargo cargo = new Cargo("TEST-001", "Test Owner", 1000.0);
        
        // Verify that the correct SQL was executed with the right parameters
        assertEquals("INSERT OR REPLACE INTO cargo (cargo_id, owner_name, capacity, used_capacity) VALUES (?, ?, ?, ?)", 
                testDbConnection.getLastPreparedSql());
        verify(mockPreparedStatement).setString(1, "TEST-001");
        verify(mockPreparedStatement).setString(2, "Test Owner");
        verify(mockPreparedStatement).setDouble(3, 1000.0);
        verify(mockPreparedStatement).setDouble(4, 0.0);
        verify(mockPreparedStatement).executeUpdate();
    }
    
    @Test
    @DisplayName("Test updating cargo in database")
    void testUpdateDatabase() throws SQLException {
        // Create a cargo object
        Cargo cargo = new Cargo("TEST-002", "Test Owner", 1000.0);
        
        // Reset invocations to clear the constructor's saveToDatabase call
        reset(mockPreparedStatement);
        
        // Add an item which will call updateDatabase()
        cargo.addItem("Test Item", 10, 5.0);
        
        // Verify that the correct SQL was executed with the right parameters for the update
        assertEquals("UPDATE cargo SET used_capacity = ? WHERE cargo_id = ?", 
                testDbConnection.getLastPreparedSql());
        verify(mockPreparedStatement).setDouble(1, 50.0); // 10 items * 5.0 weight = 50.0
        verify(mockPreparedStatement).setString(2, "TEST-002");
        verify(mockPreparedStatement).executeUpdate();
    }
    
    @Test
    @DisplayName("Test handling database exception")
    void testDatabaseException() throws SQLException {
        // Make the prepared statement throw an exception when executeUpdate is called
        doThrow(new SQLException("Test database error")).when(mockPreparedStatement).executeUpdate();
        
        // Create a cargo object which will call saveToDatabase()
        // This should not throw an exception as the method catches and logs it
        assertDoesNotThrow(() -> {
            Cargo cargo = new Cargo("TEST-003", "Test Owner", 1000.0);
        });
    }
    
    /**
     * A test implementation of Connection that avoids the need to mock the interface directly
     */
    private static class TestDatabaseConnection implements Connection {
        private final PreparedStatement mockPreparedStatement;
        private String lastPreparedSql;
        private boolean autoCommit = false;
        
        public TestDatabaseConnection(PreparedStatement mockPreparedStatement) {
            this.mockPreparedStatement = mockPreparedStatement;
        }
        
        public String getLastPreparedSql() {
            return lastPreparedSql;
        }
        
        @Override
        public PreparedStatement prepareStatement(String sql) {
            this.lastPreparedSql = sql;
            return mockPreparedStatement;
        }
        
        @Override
        public void close() {
            // Do nothing for test
        }
        
        @Override
        public boolean getAutoCommit() {
            return autoCommit;
        }
        
        @Override
        public void setAutoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
        }
        
        @Override
        public void commit() {
            // Do nothing for test
        }
        
        // Implement other methods with default implementations
        // Only implement the methods that are actually used in the tests
        
        // The following methods are not used in our tests, so we provide empty implementations
        @Override public java.sql.Statement createStatement() { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.CallableStatement prepareCall(String sql) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public String nativeSQL(String sql) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public void rollback() { /* Do nothing */ }
        @Override public boolean isClosed() { return false; }
        @Override public java.sql.DatabaseMetaData getMetaData() { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public boolean isReadOnly() { return false; }
        @Override public void setReadOnly(boolean readOnly) { /* Do nothing */ }
        @Override public String getCatalog() { return null; }
        @Override public void setCatalog(String catalog) { /* Do nothing */ }
        @Override public int getTransactionIsolation() { return Connection.TRANSACTION_NONE; }
        @Override public void setTransactionIsolation(int level) { /* Do nothing */ }
        @Override public java.sql.SQLWarning getWarnings() { return null; }
        @Override public void clearWarnings() { /* Do nothing */ }
        @Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.util.Map<String, Class<?>> getTypeMap() { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public void setTypeMap(java.util.Map<String, Class<?>> map) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public int getHoldability() { return 0; }
        @Override public void setHoldability(int holdability) { /* Do nothing */ }
        @Override public java.sql.Savepoint setSavepoint() { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.Savepoint setSavepoint(String name) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public void rollback(java.sql.Savepoint savepoint) { /* Do nothing */ }
        @Override public void releaseSavepoint(java.sql.Savepoint savepoint) { /* Do nothing */ }
        @Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.Clob createClob() { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.Blob createBlob() { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.NClob createNClob() { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.SQLXML createSQLXML() { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public boolean isValid(int timeout) { return true; }
        @Override public void setClientInfo(String name, String value) { /* Do nothing */ }
        @Override public String getClientInfo(String name) { return null; }
        @Override public java.util.Properties getClientInfo() { return new java.util.Properties(); }
        @Override public void setClientInfo(java.util.Properties properties) { /* Do nothing */ }
        @Override public java.sql.Array createArrayOf(String typeName, Object[] elements) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public java.sql.Struct createStruct(String typeName, Object[] attributes) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public String getSchema() { return null; }
        @Override public void setSchema(String schema) { /* Do nothing */ }
        @Override public void abort(java.util.concurrent.Executor executor) { /* Do nothing */ }
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) { /* Do nothing */ }
        @Override public int getNetworkTimeout() { return 0; }
        @Override public <T> T unwrap(Class<T> iface) { throw new UnsupportedOperationException("Not implemented for test"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
