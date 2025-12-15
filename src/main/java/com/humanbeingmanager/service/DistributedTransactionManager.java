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
    
    // Phase 1: prepare minio - загружаем файл в минио с временым ключом
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String prepareMinIO(InputStream inputStream, String contentType, long size) {
        String transactionId = generateTransactionId();
        lock.lock();
        try {
            LOGGER.info("2PC Coordinator [BEGIN] - Transaction started: " + transactionId);
            
            TransactionState state = new TransactionState(transactionId);
            
            try {
                String tempKey = minIOService.uploadFileTemporary(inputStream, contentType, size);
                state.setTempFileKey(tempKey);
                state.setMinIOPrepared(true);
                
                transactionStates.put(transactionId, state);
                
                LOGGER.info("2PC Coordinator [PREPARE-OK] - MinIO RM: READY for transaction: " + transactionId);
                
                return transactionId;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "2PC Coordinator [PREPARE-FAIL] - MinIO RM: NOT READY for transaction: " + transactionId, e);
                sessionContext.setRollbackOnly();
                throw new RuntimeException("Failed to prepare MinIO transaction", e);
            }
        } finally {
            lock.unlock();
        }
    }
    
    //Phase 1: prepare db - check conncetivity returns transactionid on success 
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean checkDatabaseReadiness(String transactionId) {
        try {
            Long count = em.createQuery("SELECT COUNT(ih) FROM ImportHistory ih", Long.class)
                          .getSingleResult();
            
            em.flush();

            sessionContext.setRollbackOnly();
            
            LOGGER.info("Phase 1 (Prepare) - Database readiness check: READY for transaction: " + transactionId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Phase 1 (Prepare) - Database readiness check: NOT READY for transaction: " + transactionId, e);
            sessionContext.setRollbackOnly();
            return false;
        }
    }
    

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean prepareDatabase(String transactionId) {
        lock.lock();
        try {
            TransactionState state = transactionStates.get(transactionId);
            if (state == null) {
                throw new IllegalStateException("Transaction state not found: " + transactionId);
            }

            boolean isReady = checkDatabaseReadiness(transactionId);
            
            if (isReady) {
                state.setDbPrepared(true);
                LOGGER.info("2PC Coordinator [PREPARE-OK] - Database RM: READY for transaction: " + transactionId);
                return true;
            } else {
                LOGGER.severe("2PC Coordinator [PREPARE-FAIL] - Database RM: NOT READY for transaction: " + transactionId);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }
    
    //Phase 2 - commit both
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
            
            LOGGER.info("2PC Coordinator [DECISION: COMMIT] - All RMs prepared, committing transaction: " + transactionId);
            
            try {
                String finalKey = minIOService.commitFile(state.getTempFileKey());
                
                commitDatabase(transactionId);
                
                state.setCommitted(true);
                
                LOGGER.info("2PC Coordinator [COMMIT-COMPLETE] - Transaction committed: " + transactionId + ", Final key: " + finalKey);
                
                transactionStates.remove(transactionId);
                
                return finalKey;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "2PC Coordinator [COMMIT-FAIL] - Commit failed, rolling back transaction: " + transactionId, e);
                rollbackMinIO(transactionId);
                rollbackDatabase(transactionId);
                throw new RuntimeException("Failed to commit transaction", e);
            }
        } finally {
            lock.unlock();
        }
    }
    
    //откатить оба
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void rollback(String transactionId) {
        lock.lock();
        try {
            TransactionState state = transactionStates.get(transactionId);
            if (state == null) {
                LOGGER.warning("Transaction state not found for rollback: " + transactionId);
                return;
            }
            
            LOGGER.info("2PC Coordinator [DECISION: ROLLBACK] - Rolling back transaction: " + transactionId);
            
            rollbackMinIO(transactionId);
            
            sessionContext.setRollbackOnly();
            
            transactionStates.remove(transactionId);
            
            LOGGER.info("2PC Coordinator [ROLLBACK-COMPLETE] - Transaction rolled back: " + transactionId);
        } finally {
            lock.unlock();
        }
    }
    
    //откат минио
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
    

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void handleMinIOFailure(String transactionId) {
        LOGGER.severe("MinIO failure detected for transaction: " + transactionId);
        sessionContext.setRollbackOnly();
        rollbackMinIO(transactionId);
        transactionStates.remove(transactionId);
    }
    
    //коммит бд
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void commitDatabase(String transactionId) {
        if (sessionContext.getRollbackOnly()) {
            LOGGER.warning("Phase 2 (Commit) - Database: Transaction was marked for rollback, cannot commit: " + transactionId);
            throw new IllegalStateException("Cannot commit database transaction - it was marked for rollback");
        }
        LOGGER.info("Phase 2 (Commit) - Database: Transaction will be committed: " + transactionId);
    }
    
    //помечает транзакцию для отката
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void rollbackDatabase(String transactionId) {
        sessionContext.setRollbackOnly();
        LOGGER.info("Phase 2 (Rollback) - Database: Transaction marked for rollback: " + transactionId);
    }
    
    //откат если отвалилась бд
    public void handleDatabaseFailure(String transactionId) {
        LOGGER.severe("Database failure detected for transaction: " + transactionId);
        rollbackMinIO(transactionId);
        rollbackDatabase(transactionId);
        transactionStates.remove(transactionId);
    }

    private String generateTransactionId() {
        return "txn_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId() + "_" + 
               java.util.UUID.randomUUID().toString().substring(0, 8);
    }
    

    public TransactionState getTransactionState(String transactionId) {
        return transactionStates.get(transactionId);
    }
}






