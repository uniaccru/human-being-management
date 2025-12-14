package com.humanbeingmanager.service;

import com.humanbeingmanager.entity.ImportHistory;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.SessionContext;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;

@Stateless
public class DistributedTransactionManager {
    
    private static final Logger LOGGER = Logger.getLogger(DistributedTransactionManager.class.getName());
    
    @EJB
    private MinIOService minIOService;
    
    @Resource
    private SessionContext sessionContext;
    
    @PersistenceContext(unitName = "HumanBeingPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager em;
    
    // Thread-safe storage for transaction state
    private static final ConcurrentHashMap<String, TransactionState> transactionStates = new ConcurrentHashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();
    
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
            // 2PC Coordinator Log: BEGIN
            LOGGER.info("2PC Coordinator [BEGIN] - Transaction started: " + transactionId);
            
            TransactionState state = new TransactionState(transactionId);
            
            try {
                // Upload file with temporary key
                String tempKey = minIOService.uploadFileTemporary(inputStream, contentType, size);
                state.setTempFileKey(tempKey);
                state.setMinIOPrepared(true);
                
                transactionStates.put(transactionId, state);
                
                // 2PC Coordinator Log: PREPARE-OK from MinIO RM
                LOGGER.info("2PC Coordinator [PREPARE-OK] - MinIO RM: READY for transaction: " + transactionId);
                
                return transactionId;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "2PC Coordinator [PREPARE-FAIL] - MinIO RM: NOT READY for transaction: " + transactionId, e);
                // Mark transaction for rollback
                sessionContext.setRollbackOnly();
                throw new RuntimeException("Failed to prepare MinIO transaction", e);
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Phase 1: Check database readiness as resource
     * Tests database connectivity without modifying data
     * @param transactionId Transaction ID
     * @return true if database is ready, false otherwise
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean checkDatabaseReadiness(String transactionId) {
        try {
            // Simple query to test database connectivity
            Long count = em.createQuery("SELECT COUNT(ih) FROM ImportHistory ih", Long.class)
                          .getSingleResult();
            
            // Force flush to ensure connection is tested
            em.flush();
            
            // Rollback test transaction
            sessionContext.setRollbackOnly();
            
            LOGGER.info("Phase 1 (Prepare) - Database readiness check: READY for transaction: " + transactionId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Phase 1 (Prepare) - Database readiness check: NOT READY for transaction: " + transactionId, e);
            sessionContext.setRollbackOnly();
            return false;
        }
    }
    
    /**
     * Phase 1: Prepare - Mark database as prepared after readiness check
     * This method marks database as prepared in coordinator state
     * @param transactionId Transaction ID
     * @return true if database is prepared, false if not ready
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean prepareDatabase(String transactionId) {
        lock.lock();
        try {
            TransactionState state = transactionStates.get(transactionId);
            if (state == null) {
                throw new IllegalStateException("Transaction state not found: " + transactionId);
            }
            
            // Check database readiness first
            boolean isReady = checkDatabaseReadiness(transactionId);
            
            if (isReady) {
                state.setDbPrepared(true);
                // 2PC Coordinator Log: PREPARE-OK from Database RM
                LOGGER.info("2PC Coordinator [PREPARE-OK] - Database RM: READY for transaction: " + transactionId);
                return true;
            } else {
                // 2PC Coordinator Log: PREPARE-FAIL from Database RM
                LOGGER.severe("2PC Coordinator [PREPARE-FAIL] - Database RM: NOT READY for transaction: " + transactionId);
                return false;
            }
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
            
            // 2PC Coordinator Log: DECISION - COMMIT
            LOGGER.info("2PC Coordinator [DECISION: COMMIT] - All RMs prepared, committing transaction: " + transactionId);
            
            try {
                // Commit MinIO: rename from temp/ to final location
                String finalKey = minIOService.commitFile(state.getTempFileKey());
                
                // Commit database transaction explicitly
                commitDatabase(transactionId);
                
                state.setCommitted(true);
                
                // 2PC Coordinator Log: COMMIT completed
                LOGGER.info("2PC Coordinator [COMMIT-COMPLETE] - Transaction committed: " + transactionId + ", Final key: " + finalKey);
                
                // Clean up transaction state after successful commit
                transactionStates.remove(transactionId);
                
                return finalKey;
            } catch (Exception e) {
                // 2PC Coordinator Log: COMMIT failed, rolling back
                LOGGER.log(Level.SEVERE, "2PC Coordinator [COMMIT-FAIL] - Commit failed, rolling back transaction: " + transactionId, e);
                // Rollback both resources
                rollbackMinIO(transactionId);
                rollbackDatabase(transactionId);
                throw new RuntimeException("Failed to commit transaction", e);
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
            
            // 2PC Coordinator Log: DECISION - ROLLBACK
            LOGGER.info("2PC Coordinator [DECISION: ROLLBACK] - Rolling back transaction: " + transactionId);
            
            // Rollback MinIO
            rollbackMinIO(transactionId);
            
            // Rollback database (via session context)
            sessionContext.setRollbackOnly();
            
            // Clean up transaction state
            transactionStates.remove(transactionId);
            
            // 2PC Coordinator Log: ROLLBACK completed
            LOGGER.info("2PC Coordinator [ROLLBACK-COMPLETE] - Transaction rolled back: " + transactionId);
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
     * Phase 2: Commit database transaction explicitly
     * Database transaction is already in progress from Phase 1 (import)
     * This method ensures the transaction will commit (by not calling setRollbackOnly)
     * @param transactionId Transaction ID
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void commitDatabase(String transactionId) {
        // Database commit happens automatically when the JTA transaction completes
        // The import transaction from Phase 1 will be committed when this method returns
        // We just need to ensure rollbackOnly is not set
        if (sessionContext.getRollbackOnly()) {
            LOGGER.warning("Phase 2 (Commit) - Database: Transaction was marked for rollback, cannot commit: " + transactionId);
            throw new IllegalStateException("Cannot commit database transaction - it was marked for rollback");
        }
        LOGGER.info("Phase 2 (Commit) - Database: Transaction will be committed: " + transactionId);
    }
    
    /**
     * Rollback database transaction
     * Marks current JTA transaction for rollback
     * @param transactionId Transaction ID
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void rollbackDatabase(String transactionId) {
        sessionContext.setRollbackOnly();
        LOGGER.info("Phase 2 (Rollback) - Database: Transaction marked for rollback: " + transactionId);
    }
    
    /**
     * Handle database failure - rollback MinIO
     */
    public void handleDatabaseFailure(String transactionId) {
        LOGGER.severe("Database failure detected for transaction: " + transactionId);
        rollbackMinIO(transactionId);
        rollbackDatabase(transactionId);
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






