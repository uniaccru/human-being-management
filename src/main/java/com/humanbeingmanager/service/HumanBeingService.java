package com.humanbeingmanager.service;

import com.humanbeingmanager.dao.HumanBeingDao;
import com.humanbeingmanager.dao.CarDao;
import com.humanbeingmanager.entity.HumanBeing;
import com.humanbeingmanager.entity.Car;
import com.humanbeingmanager.entity.Coordinates;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

@ApplicationScoped
public class HumanBeingService {

    private static final Logger LOGGER = Logger.getLogger(HumanBeingService.class.getName());

    @Inject
    private HumanBeingDao humanBeingDao;

    @Inject
    private CarDao carDao;

    @Inject
    private Validator validator;

    public HumanBeing createHumanBeing(HumanBeing humanBeing) throws ValidationException {
        LOGGER.log(Level.INFO, "Creating new HumanBeing: {0}", humanBeing.getName());
        
        validateHumanBeing(humanBeing);
        validateBusinessRules(humanBeing);
        
        if (humanBeing.getCar() != null && humanBeing.getCar().getId() == null) {
            Car savedCar = carDao.create(humanBeing.getCar());
            humanBeing.setCar(savedCar);
        }

        HumanBeing created = humanBeingDao.create(humanBeing);
        LOGGER.log(Level.INFO, "Successfully created HumanBeing with ID: {0}", created.getId());
        
        return created;
    }

    public HumanBeing updateHumanBeing(HumanBeing humanBeing) throws ValidationException, EntityNotFoundException {
        LOGGER.log(Level.INFO, "Updating HumanBeing with ID: {0}", humanBeing.getId());
        
        if (humanBeing.getId() == null || !humanBeingDao.existsById(humanBeing.getId())) {
            throw new EntityNotFoundException("HumanBeing with ID " + humanBeing.getId() + " not found");
        }
        
        validateHumanBeing(humanBeing);
        validateBusinessRules(humanBeing);

        if (humanBeing.getCar() != null) {
            if (humanBeing.getCar().getId() == null) {
                Car savedCar = carDao.create(humanBeing.getCar());
                humanBeing.setCar(savedCar);
            } else {
                carDao.update(humanBeing.getCar());
            }
        }
        
        HumanBeing updated = humanBeingDao.update(humanBeing);
        LOGGER.log(Level.INFO, "Successfully updated HumanBeing with ID: {0}", updated.getId());
        
        return updated;
    }

    public Optional<HumanBeing> getHumanBeingById(Long id) {
        LOGGER.log(Level.INFO, "Retrieving HumanBeing with ID: {0}", id);
        return humanBeingDao.findById(id);
    }

    public List<HumanBeing> getAllHumanBeings() {
        LOGGER.log(Level.INFO, "Retrieving all HumanBeings");
        return humanBeingDao.findAll();
    }

    public List<HumanBeing> getAllHumanBeings(int page, int size) {
        LOGGER.log(Level.INFO, "Retrieving HumanBeings (page: {0}, size: {1})", new Object[]{page, size});
        return humanBeingDao.findAll(page, size);
    }

    public List<HumanBeing> getAllHumanBeings(int page, int size, String filterColumn, String filterValue, String sortColumn, String sortDirection) {
        LOGGER.log(Level.INFO, "Retrieving HumanBeings (page: {0}, size: {1}, filter: {2}={3}, sort: {4} {5})", 
                  new Object[]{page, size, filterColumn, filterValue, sortColumn, sortDirection});
        return humanBeingDao.findAll(page, size, filterColumn, filterValue, sortColumn, sortDirection);
    }

    public boolean deleteHumanBeing(Long id) throws EntityNotFoundException {
        LOGGER.log(Level.INFO, "Deleting HumanBeing with ID: {0}", id);
        
        Optional<HumanBeing> humanBeing = humanBeingDao.findById(id);
        if (humanBeing.isEmpty()) {
            throw new EntityNotFoundException("HumanBeing with ID " + id + " not found");
        }
        
        boolean deleted = humanBeingDao.deleteById(id);
        
        if (deleted) {
            LOGGER.log(Level.INFO, "Successfully deleted HumanBeing with ID: {0}", id);
        }
        
        return deleted;
    }

    public Long getHumanBeingCount() {
        return humanBeingDao.count();
    }

    public Long getHumanBeingCount(String filterColumn, String filterValue) {
        return humanBeingDao.count(filterColumn, filterValue);
    }

    public List<HumanBeing> findByMood(String mood) {
        LOGGER.log(Level.INFO, "Finding HumanBeings by mood: {0}", mood);
        return humanBeingDao.findByMood(mood);
    }
    public List<Car> getAllCars() {
        LOGGER.log(Level.INFO, "Retrieving all Cars for linking");
        return carDao.findAll();
    }

    //s(v)o

    public Long getSumOfMinutesWaiting() {
        LOGGER.log(Level.INFO, "Calculating sum of minutes waiting");
        return humanBeingDao.getSumOfMinutesWaiting();
    }

    public HumanBeing getMaxToothpick() {
        LOGGER.log(Level.INFO, "Getting HumanBeing with max toothpick value");
        return humanBeingDao.getMaxToothpick();
    }

    public List<HumanBeing> getSoundtrackStartsWith(String substring) {
        LOGGER.log(Level.INFO, "Getting HumanBeings with soundtrack starting with: {0}", substring);
        return humanBeingDao.getSoundtrackStartsWith(substring);
    }

    public int deleteHeroesWithoutToothpicks() {
        LOGGER.log(Level.INFO, "Deleting all heroes without toothpicks");
        return humanBeingDao.deleteHeroesWithoutToothpicks();
    }

    public int setAllMoodToSadness() {
        LOGGER.log(Level.INFO, "Setting all heroes mood to SADNESS");
        return humanBeingDao.setAllMoodToSadness();
    }


    private void validateHumanBeing(HumanBeing humanBeing) throws ValidationException {
        Set<ConstraintViolation<HumanBeing>> violations = validator.validate(humanBeing);
        
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Validation failed: ");
            for (ConstraintViolation<HumanBeing> violation : violations) {
                errorMessage.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
            }
            throw new ValidationException(errorMessage.toString());
        }
    }

    private void validateBusinessRules(HumanBeing humanBeing) throws ValidationException {
        StringBuilder errors = new StringBuilder();
        
        if (humanBeing.getName() != null) {
            String name = humanBeing.getName().trim();
            if (name.length() > 100) {
                errors.append("Name must be 100 characters or less; ");
            }
            if (!name.matches("^[a-zA-Z0-9\\s\\-_.]+$")) {
                errors.append("Name can only contain letters, numbers, spaces, hyphens, underscores, and periods; ");
            }
        }

        if (humanBeing.getCoordinates() != null) {
            Coordinates coords = humanBeing.getCoordinates();
            
            if (coords.getX() != null) {
                if (coords.getX() < -1000 || coords.getX() > 1000) {
                    errors.append("X coordinate must be between -1000 and 1000; ");
                }
                if (coords.getX() == 0) {
                    errors.append("X coordinate cannot be zero; ");
                }
            }
            
            if (coords.getY() <= -1000) {
                errors.append("Y coordinate must be greater than -1000; ");
            }
            if (coords.getY() > 1000) {
                errors.append("Y coordinate must be at most 1000; ");
            }
            if (coords.getY() == 0) {
                errors.append("Y coordinate cannot be zero; ");
            }
        }

        if (humanBeing.isRealHero() && humanBeing.getImpactSpeed() < 0) {
            errors.append("Real heroes cannot have negative impact speed; ");
        }
        if (humanBeing.getImpactSpeed() < -1000 || humanBeing.getImpactSpeed() > 1000) {
            errors.append("Impact speed must be between -1000 and 1000; ");
        }

        if (humanBeing.getMinutesOfWaiting() != null) {
            if (humanBeing.getMinutesOfWaiting() < 0) {
                errors.append("Minutes of waiting cannot be negative; ");
            }
            if (humanBeing.getMinutesOfWaiting() > 99999) {
                errors.append("Minutes of waiting must be less than 100,000; ");
            }
        }

        if (humanBeing.getSoundtrackName() != null) {
            String soundtrack = humanBeing.getSoundtrackName().trim();
            if (soundtrack.length() > 100) {
                errors.append("Soundtrack name must be 100 characters or less; ");
            }
            if (!soundtrack.matches("^[a-zA-Z0-9\\s\\-_.]+$")) {
                errors.append("Soundtrack name can only contain letters, numbers, spaces, hyphens, underscores, and periods; ");
            }
        }

        if (humanBeing.getCar() != null) {
            Car car = humanBeing.getCar();
            if (car.getName() != null && car.getName().length() > 50) {
                errors.append("Car name must be 50 characters or less; ");
            }
            if (car.getName() != null && !car.getName().matches("^[a-zA-Z0-9\\s\\-_.]*$")) {
                errors.append("Car name can only contain letters, numbers, spaces, hyphens, underscores, and periods; ");
            }
        }

        if (errors.length() > 0) {
            throw new ValidationException(errors.toString());
        }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class EntityNotFoundException extends Exception {
        public EntityNotFoundException(String message) {
            super(message);
        }
    }
}
