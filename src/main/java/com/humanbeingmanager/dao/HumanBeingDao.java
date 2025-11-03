package com.humanbeingmanager.dao;

import com.humanbeingmanager.entity.HumanBeing;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class HumanBeingDao {

    @Inject
    private EntityManager entityManager;

    public HumanBeing create(HumanBeing humanBeing) {
        EntityTransaction transaction = entityManager.getTransaction();
        boolean transactionStartedHere = false;
        try {
            if (!transaction.isActive()) {
                transaction.begin();
                transactionStartedHere = true;
            }
            entityManager.persist(humanBeing);
            entityManager.flush();
            // Коммитим только если мы начали транзакцию здесь
            if (transactionStartedHere && transaction.isActive()) {
                transaction.commit();
            }
            return humanBeing;
        } catch (Exception e) {
            // Откатываем только если мы начали транзакцию здесь
            if (transactionStartedHere && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public Optional<HumanBeing> findById(Long id) {
        TypedQuery<HumanBeing> query = entityManager.createNamedQuery("HumanBeing.findById", HumanBeing.class);
        query.setParameter("id", id);
        List<HumanBeing> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<HumanBeing> findAll() {
        TypedQuery<HumanBeing> query = entityManager.createNamedQuery("HumanBeing.findAll", HumanBeing.class);
        return query.getResultList();
    }

    public List<HumanBeing> findAll(int page, int size) {
        TypedQuery<HumanBeing> query = entityManager.createNamedQuery("HumanBeing.findAll", HumanBeing.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    //filtering
    public List<HumanBeing> findAll(int page, int size, String filterColumn, String filterValue, String sortColumn, String sortDirection) {
        StringBuilder queryBuilder = new StringBuilder("SELECT h FROM HumanBeing h");
        
        if (filterColumn != null && filterValue != null && !filterValue.trim().isEmpty()) {
            queryBuilder.append(" WHERE h.").append(filterColumn).append(" = :filterValue");
        }
        
        //sorting
        if (sortColumn != null && !sortColumn.trim().isEmpty()) {
            queryBuilder.append(" ORDER BY h.").append(sortColumn);
            if ("desc".equalsIgnoreCase(sortDirection)) {
                queryBuilder.append(" DESC");
            } else {
                queryBuilder.append(" ASC");
            }
        }
        
        TypedQuery<HumanBeing> query = entityManager.createQuery(queryBuilder.toString(), HumanBeing.class);
        
        if (filterColumn != null && filterValue != null && !filterValue.trim().isEmpty()) {
            query.setParameter("filterValue", filterValue);
        }
        
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public HumanBeing update(HumanBeing humanBeing) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            if (!transaction.isActive()) {
                transaction.begin();
            }
            HumanBeing merged = entityManager.merge(humanBeing);
            entityManager.flush();
            if (transaction.isActive()) {
                transaction.commit();
            }
            return merged;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public boolean deleteById(Long id) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            if (!transaction.isActive()) {
                transaction.begin();
            }
            Optional<HumanBeing> humanBeing = findById(id);
            if (humanBeing.isPresent()) {
                entityManager.remove(humanBeing.get());
                entityManager.flush();
                if (transaction.isActive()) {
                    transaction.commit();
                }
                return true;
            }
            if (transaction.isActive()) {
                transaction.commit();
            }
            return false;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public void delete(HumanBeing humanBeing) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            if (!transaction.isActive()) {
                transaction.begin();
            }
            if (entityManager.contains(humanBeing)) {
                entityManager.remove(humanBeing);
            } else {
                HumanBeing managedEntity = entityManager.merge(humanBeing);
                entityManager.remove(managedEntity);
            }
            entityManager.flush();
            if (transaction.isActive()) {
                transaction.commit();
            }
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public Long count() {
        TypedQuery<Long> query = entityManager.createNamedQuery("HumanBeing.countAll", Long.class);
        return query.getSingleResult();
    }


    //count with filtering
    public Long count(String filterColumn, String filterValue) {
        StringBuilder queryBuilder = new StringBuilder("SELECT COUNT(h) FROM HumanBeing h");
        
        if (filterColumn != null && filterValue != null && !filterValue.trim().isEmpty()) {
            queryBuilder.append(" WHERE h.").append(filterColumn).append(" = :filterValue");
        }
        
        TypedQuery<Long> query = entityManager.createQuery(queryBuilder.toString(), Long.class);

        if (filterColumn != null && filterValue != null && !filterValue.trim().isEmpty()) {
            query.setParameter("filterValue", filterValue);
        }
        
        return query.getSingleResult();
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }


    public List<HumanBeing> findByMood(String mood) {
        TypedQuery<HumanBeing> query = entityManager.createQuery(
            "SELECT h FROM HumanBeing h WHERE h.mood = :mood", HumanBeing.class);
        query.setParameter("mood", mood);
        return query.getResultList();
    }

    public Optional<HumanBeing> findByCoordinates(Integer x, double y, Long excludeId) {
        StringBuilder jpql = new StringBuilder(
            "SELECT h FROM HumanBeing h WHERE h.coordinates.x = :x AND h.coordinates.y = :y");
        
        if (excludeId != null) {
            jpql.append(" AND h.id != :excludeId");
        }
        
        TypedQuery<HumanBeing> query = entityManager.createQuery(jpql.toString(), HumanBeing.class);
        query.setParameter("x", x);
        query.setParameter("y", y);
        
        if (excludeId != null) {
            query.setParameter("excludeId", excludeId);
        }
        
        // Используем PESSIMISTIC_WRITE для блокировки строк при проверке уникальности координат
        // Это предотвращает race condition при одновременном создании объектов с одинаковыми координатами
        query.setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        
        List<HumanBeing> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void refresh(HumanBeing humanBeing) {
        entityManager.refresh(humanBeing);
    }

    //special Operations

    public Long getSumOfMinutesWaiting() {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT SUM(h.minutesOfWaiting) FROM HumanBeing h", Long.class);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    public HumanBeing getMaxToothpick() {
        TypedQuery<HumanBeing> query = entityManager.createQuery(
            "SELECT h FROM HumanBeing h WHERE h.hasToothpick = true ORDER BY h.id", 
            HumanBeing.class);
        List<HumanBeing> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<HumanBeing> getSoundtrackStartsWith(String substring) {
        TypedQuery<HumanBeing> query = entityManager.createQuery(
            "SELECT h FROM HumanBeing h WHERE h.soundtrackName LIKE :substring", HumanBeing.class);
        query.setParameter("substring", substring + "%");
        return query.getResultList();
    }

    public int deleteHeroesWithoutToothpicks() {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            if (!transaction.isActive()) {
                transaction.begin();
            }
            TypedQuery<HumanBeing> query = entityManager.createQuery(
                "SELECT h FROM HumanBeing h WHERE h.realHero = true AND (h.hasToothpick = false OR h.hasToothpick IS NULL)", 
                HumanBeing.class);
            List<HumanBeing> toDelete = query.getResultList();
            
            for (HumanBeing humanBeing : toDelete) {
                entityManager.remove(humanBeing);
            }
            
            entityManager.flush();
            if (transaction.isActive()) {
                transaction.commit();
            }
            return toDelete.size();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public int setAllMoodToSadness() {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            if (!transaction.isActive()) {
                transaction.begin();
            }

            TypedQuery<HumanBeing> selectQuery = entityManager.createQuery(
                "SELECT h FROM HumanBeing h WHERE h.realHero = true", HumanBeing.class);
            List<HumanBeing> heroes = selectQuery.getResultList();

            int updatedCount = 0;
            for (HumanBeing hero : heroes) {
                hero.setMood(com.humanbeingmanager.entity.Mood.SADNESS);
                entityManager.merge(hero);
                updatedCount++;
            }
            
            entityManager.flush();
            if (transaction.isActive()) {
                transaction.commit();
            }
            return updatedCount;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }
}