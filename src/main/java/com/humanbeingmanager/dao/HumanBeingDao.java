package com.humanbeingmanager.dao;

import com.humanbeingmanager.entity.HumanBeing;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Stateless
public class HumanBeingDao {

    /** Allowed HumanBeing field names for filter/sort to prevent JPQL injection. */
    private static final Set<String> ALLOWED_FILTER_SORT_COLUMNS = Set.of(
            "id", "name", "creationDate", "realHero", "hasToothpick", "mood",
            "impactSpeed", "soundtrackName", "minutesOfWaiting", "weaponType"
    );

    @PersistenceContext(unitName = "HumanBeingPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager entityManager;

    public HumanBeing create(HumanBeing humanBeing) {
        entityManager.persist(humanBeing);
        return humanBeing;
    }

    public Optional<HumanBeing> findById(Long id) {
        HumanBeing hb = entityManager.find(HumanBeing.class, id);
        return Optional.ofNullable(hb);
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

    public List<HumanBeing> findAll(int page, int size, String filterColumn, String filterValue, String sortColumn, String sortDirection) {
        StringBuilder queryBuilder = new StringBuilder("SELECT h FROM HumanBeing h");
        String safeFilterColumn = isAllowedColumn(filterColumn) ? filterColumn : null;
        String safeSortColumn = isAllowedColumn(sortColumn) ? sortColumn : null;

        if (safeFilterColumn != null && filterValue != null && !filterValue.trim().isEmpty()) {
            queryBuilder.append(" WHERE h.").append(safeFilterColumn).append(" = :filterValue");
        }

        if (safeSortColumn != null) {
            queryBuilder.append(" ORDER BY h.").append(safeSortColumn);
            if ("desc".equalsIgnoreCase(sortDirection)) {
                queryBuilder.append(" DESC");
            } else {
                queryBuilder.append(" ASC");
            }
        }

        TypedQuery<HumanBeing> query = entityManager.createQuery(queryBuilder.toString(), HumanBeing.class);

        if (safeFilterColumn != null && filterValue != null && !filterValue.trim().isEmpty()) {
            query.setParameter("filterValue", filterValue);
        }

        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    private static boolean isAllowedColumn(String column) {
        return column != null && !column.trim().isEmpty() && ALLOWED_FILTER_SORT_COLUMNS.contains(column.trim());
    }

    public HumanBeing update(HumanBeing humanBeing) {
        HumanBeing merged = entityManager.merge(humanBeing);
        return merged;
    }

    public boolean deleteById(Long id) {
        Optional<HumanBeing> humanBeing = findById(id);
        if (humanBeing.isPresent()) {
            entityManager.remove(humanBeing.get());
            return true;
        }
        return false;
    }

    public void delete(HumanBeing humanBeing) {
        if (entityManager.contains(humanBeing)) {
            entityManager.remove(humanBeing);
        } else {
            HumanBeing managedEntity = entityManager.merge(humanBeing);
            entityManager.remove(managedEntity);
        }
    }

    public Long count() {
        TypedQuery<Long> query = entityManager.createNamedQuery("HumanBeing.countAll", Long.class);
        return query.getSingleResult();
    }


    public Long count(String filterColumn, String filterValue) {
        StringBuilder queryBuilder = new StringBuilder("SELECT COUNT(h) FROM HumanBeing h");
        String safeFilterColumn = isAllowedColumn(filterColumn) ? filterColumn : null;

        if (safeFilterColumn != null && filterValue != null && !filterValue.trim().isEmpty()) {
            queryBuilder.append(" WHERE h.").append(safeFilterColumn).append(" = :filterValue");
        }

        TypedQuery<Long> query = entityManager.createQuery(queryBuilder.toString(), Long.class);

        if (safeFilterColumn != null && filterValue != null && !filterValue.trim().isEmpty()) {
            query.setParameter("filterValue", filterValue);
        }

        return query.getSingleResult();
    }

    public boolean existsById(Long id) {
        return entityManager.find(HumanBeing.class, id) != null;
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
        
        query.setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        
        try {
            query.setHint("jakarta.persistence.lock.timeout", 5000);
        } catch (Exception e) {

        }
        
        List<HumanBeing> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void refresh(HumanBeing humanBeing) {
        entityManager.refresh(humanBeing);
    }


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

    public List<HumanBeing> findHeroesWithoutToothpicks() {
        TypedQuery<HumanBeing> query = entityManager.createQuery(
            "SELECT h FROM HumanBeing h WHERE h.realHero = true AND (h.hasToothpick = false OR h.hasToothpick IS NULL)", 
            HumanBeing.class);
        return query.getResultList();
    }

    public List<HumanBeing> findAllRealHeroes() {
        TypedQuery<HumanBeing> query = entityManager.createQuery(
            "SELECT h FROM HumanBeing h WHERE h.realHero = true", HumanBeing.class);
        return query.getResultList();
    }
}