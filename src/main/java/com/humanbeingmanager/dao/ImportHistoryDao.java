package com.humanbeingmanager.dao;

import com.humanbeingmanager.entity.ImportHistory;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;

@Stateless
public class ImportHistoryDao {

    @PersistenceContext(unitName = "HumanBeingPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager entityManager;

    public ImportHistory create(ImportHistory history) {
        entityManager.persist(history);
        return history;
    }

    public List<ImportHistory> findAll() {
        TypedQuery<ImportHistory> query = entityManager.createQuery(
            "SELECT ih FROM ImportHistory ih ORDER BY ih.createdAt DESC", ImportHistory.class);
        return query.getResultList();
    }

    public List<ImportHistory> findAll(int page, int size) {
        TypedQuery<ImportHistory> query = entityManager.createQuery(
            "SELECT ih FROM ImportHistory ih ORDER BY ih.createdAt DESC", ImportHistory.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public Optional<ImportHistory> findById(Long id) {
        ImportHistory history = entityManager.find(ImportHistory.class, id);
        return Optional.ofNullable(history);
    }

    public Long count() {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(ih) FROM ImportHistory ih", Long.class);
        return query.getSingleResult();
    }
}
