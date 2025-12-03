package com.humanbeingmanager.service;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.SessionContext;
import jakarta.annotation.Resource;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Manages distributed transactions between database and MinIO using two-phase commit pattern
 */
@Stateless
public class DistributedTransactionManager {
    
    private static final Logger LOGGER = Logger.getLogger(DistributedTransactionManager.class.getName());
    
    @EJB
    private MinIOService minIOService;
    
    @Resource
    private SessionContext sessionContext;
    
    // Thread-safe storage for transaction state
    private static final ConcurrentHashMap<String, TransactionState> transactionStates = new ConcurrentHashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();
    
    /**
     * Transaction state for two-phase commit
     */
    public static class TransactionState {
        private String tempFileKey;
        private boolean minIOPrepared = false;
        private boolean dbPrepared = false;
        private boolean committed = false;
        private final String transactionId;
        
        public TransactionState(String transactionId) {
            this.transactionId = transactionId;
        }
        
        public String getTempFileKey() {
            return tempFileKey;
        }
        
        public void setTempFileKey(String tempFileKey) {
            this.tempFileKey = tempFileKey;
        }
        
        public boolean isMinIOPrepared() {
            return minIOPrepared;
        }
        
        public void setMinIOPrepared(boolean minIOPrepared) {
            this.minIOPrepared = minIOPrepared;
        }
        
        public boolean isDbPrepared() {
            return dbPrepared;
        }
        
        public void setDbPrepared(boolean dbPrepared) {
            this.dbPrepared = dbPrepared;
        }
        
        public boolean isCommitted() {
            return committed;
        }
        
        public void setCommitted(boolean committed) {
            this.committed = committed;
        }
        
        public String getTransactionId() {
            return transactionId;
        }
    }
    
    /**
     * Phase 1: Prepare - Upload file to MinIO with temporary key
     * @param inputStream File input stream
     * @param contentType Content type
     * @param size File size
     * @return Transaction ID
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String prepareMinIO(InputStream inputStream, String contentType, long size) {
        String transactionId = generateTransactionId();
        lock.lock();
        try {
            TransactionState state = new TransactionState(transactionId);
            
            try {
                // Upload file with temporary key
                String tempKey = minIOService.uploadFileTemporary(inputStream, contentType, size);
                state.setTempFileKey(tempKey);
                state.setMinIOPrepared(true);
                
                transactionStates.put(transactionId, state);
                LOGGER.info("Phase 1 (Prepare) - MinIO: File uploaded with temp key: " + tempKey);
                
                return transactionId;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Phase 1 (Prepare) - MinIO failed", e);
                // Mark transaction for rollback
                sessionContext.setRollbackOnly();
                throw new RuntimeException("Failed to prepare MinIO transaction", e);
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Phase 1: Prepare - Mark database as prepared
     * @param transactionId Transaction ID
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void prepareDatabase(String transactionId) {
        lock.lock();
        try {
            TransactionState state = transactionStates.get(transactionId);
            if (state == null) {
                throw new IllegalStateException("Transaction state not found: " + transactionId);
            }
            
            state.setDbPrepared(true);
            LOGGER.info("Phase 1 (Prepare) - Database: Transaction prepared: " + transactionId);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Phase 2: Commit - Commit both MinIO and database transactions
     * @param transactionId Transaction ID
     * @return Final file key
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String commit(String transactionId) {
        lock.lock();
        try {
            TransactionState state = transactionStates.get(transactionId);
            if (state == null) {
                throw new IllegalStateException("Transaction state not found: " + transactionId);
            }
            
            if (!state.isMinIOPrepared() || !state.isDbPrepared()) {
                throw new IllegalStateException("Transaction not fully prepared: " + transactionId);
            }
            
            try {
                // Commit MinIO: rename from temp/ to final location
                String finalKey = minIOService.commitFile(state.getTempFileKey());
                state.setCommitted(true);
                
                LOGGER.info("Phase 2 (Commit) - Transaction committed: " + transactionId + ", Final key: " + finalKey);
                
                // Clean up transaction state after successful commit
                transactionStates.remove(transactionId);
                
                return finalKey;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Phase 2 (Commit) - MinIO commit failed", e);
                // Rollback database transaction
                sessionContext.setRollbackOnly();
                // Try to clean up MinIO
                rollbackMinIO(transactionId);
                throw new RuntimeException("Failed to commit MinIO transaction", e);
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Rollback - Rollback both MinIO and database transactions
     * @param transactionId Transaction ID
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void rollback(String transactionId) {
        lock.lock();
        try {
            TransactionState state = transactionStates.get(transactionId);
            if (state == null) {
                LOGGER.warning("Transaction state not found for rollback: " + transactionId);
                return;
            }
            
            // Rollback MinIO
            rollbackMinIO(transactionId);
            
            // Rollback database (via session context)
            sessionContext.setRollbackOnly();
            
            // Clean up transaction state
            transactionStates.remove(transactionId);
            
            LOGGER.info("Transaction rolled back: " + transactionId);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Rollback MinIO only (called when database transaction fails)
     */
    private void rollbackMinIO(String transactionId) {
        TransactionState state = transactionStates.get(transactionId);
        if (state != null && state.getTempFileKey() != null) {
            try {
                minIOService.deleteFile(state.getTempFileKey());
                LOGGER.info("MinIO rollback: Deleted temp file: " + state.getTempFileKey());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to delete temp file during rollback: " + state.getTempFileKey(), e);
            }
        }
    }
    
    /**
     * Handle MinIO failure - rollback database
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void handleMinIOFailure(String transactionId) {
        LOGGER.severe("MinIO failure detected for transaction: " + transactionId);
        sessionContext.setRollbackOnly();
        rollbackMinIO(transactionId);
        transactionStates.remove(transactionId);
    }
    
    /**
     * Handle database failure - rollback MinIO
     */
    public void handleDatabaseFailure(String transactionId) {
        LOGGER.severe("Database failure detected for transaction: " + transactionId);
        rollbackMinIO(transactionId);
        transactionStates.remove(transactionId);
    }
    
    /**
     * Handle business logic failure - rollback both
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void handleBusinessLogicFailure(String transactionId) {
        LOGGER.severe("Business logic failure detected for transaction: " + transactionId);
        rollback(transactionId);
    }
    
    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        return "txn_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId() + "_" + 
               java.util.UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Get transaction state (for debugging)
     */
    public TransactionState getTransactionState(String transactionId) {
        return transactionStates.get(transactionId);
    }
}



