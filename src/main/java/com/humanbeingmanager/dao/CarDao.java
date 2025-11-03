package com.humanbeingmanager.dao;

import com.humanbeingmanager.entity.Car;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CarDao {

    @Inject
    private EntityManager entityManager;

    public Car create(Car car) {
        EntityTransaction transaction = entityManager.getTransaction();
        boolean transactionStartedHere = false;
        try {
            if (!transaction.isActive()) {
                transaction.begin();
                transactionStartedHere = true;
            }
            entityManager.persist(car);
            entityManager.flush();
            // Коммитим только если мы начали транзакцию здесь
            if (transactionStartedHere && transaction.isActive()) {
                transaction.commit();
            }
            return car;
        } catch (Exception e) {
            // Откатываем только если мы начали транзакцию здесь
            if (transactionStartedHere && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public Optional<Car> findById(Long id) {
        Car car = entityManager.find(Car.class, id);
        return Optional.ofNullable(car);
    }

    public List<Car> findAll() {
        TypedQuery<Car> query = entityManager.createQuery("SELECT c FROM Car c", Car.class);
        return query.getResultList();
    }

    public Car update(Car car) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            if (!transaction.isActive()) {
                transaction.begin();
            }
            Car merged = entityManager.merge(car);
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
        Optional<Car> car = findById(id);
        if (car.isPresent()) {
            entityManager.remove(car.get());
            entityManager.flush();
            return true;
        }
        return false;
    }

    public void delete(Car car) {
        if (entityManager.contains(car)) {
            entityManager.remove(car);
        } else {
            Car managedEntity = entityManager.merge(car);
            entityManager.remove(managedEntity);
        }
        entityManager.flush();
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    public List<Car> findByNameContaining(String name) {
        TypedQuery<Car> query = entityManager.createQuery(
            "SELECT c FROM Car c WHERE LOWER(c.name) LIKE LOWER(:name)", Car.class);
        query.setParameter("name", "%" + name + "%");
        return query.getResultList();
    }

    public void refresh(Car car) {
        entityManager.refresh(car);
    }
}