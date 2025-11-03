package com.humanbeingmanager.dao;

import com.humanbeingmanager.entity.Car;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;

@Stateless
public class CarDao {

    @PersistenceContext(unitName = "HumanBeingPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager entityManager;

    public Car create(Car car) {
        entityManager.persist(car);
        return car;
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
        Car merged = entityManager.merge(car);
        return merged;
    }

    public boolean deleteById(Long id) {
        Optional<Car> car = findById(id);
        if (car.isPresent()) {
            entityManager.remove(car.get());
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