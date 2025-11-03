package com.humanbeingmanager.service;

import com.humanbeingmanager.dao.HumanBeingDao;
import com.humanbeingmanager.dao.CarDao;
import com.humanbeingmanager.entity.HumanBeing;
import com.humanbeingmanager.entity.Car;
import com.humanbeingmanager.entity.Coordinates;
import com.humanbeingmanager.entity.Mood;
import com.humanbeingmanager.entity.WeaponType;
import com.humanbeingmanager.dto.HumanBeingDto;
import com.humanbeingmanager.dto.CarDto;
import com.humanbeingmanager.dto.CoordinatesDto;
import com.humanbeingmanager.mapper.EntityDtoMapper;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.SessionContext;
import jakarta.ejb.EJB;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class HumanBeingService {

    private static final Logger LOGGER = Logger.getLogger(HumanBeingService.class.getName());

    @EJB
    private HumanBeingDao humanBeingDao;

    @EJB
    private CarDao carDao;

    @Inject
    private Validator validator;

    @Inject
    private EntityDtoMapper mapper;

    @Resource
    private SessionContext sessionContext;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public HumanBeing createHumanBeing(HumanBeing humanBeing) throws ValidationException {
        LOGGER.log(Level.INFO, "Creating new HumanBeing: {0}", humanBeing.getName());
        
        try {
            if (humanBeing.getCreationDate() == null) {
                humanBeing.setCreationDate(new java.util.Date());
            }

            applyMachineGunDefault(humanBeing);
            
            validateHumanBeing(humanBeing);
            validateBusinessRules(humanBeing, false, null);
            
            if (humanBeing.getCar() != null && humanBeing.getCar().getId() == null) {
                Car savedCar = carDao.create(humanBeing.getCar());
                humanBeing.setCar(savedCar);
            }

            HumanBeing created = humanBeingDao.create(humanBeing);
            
            LOGGER.log(Level.INFO, "Successfully created HumanBeing with ID: {0}", created.getId());
            
            return created;
        } catch (ValidationException e) {
            sessionContext.setRollbackOnly();
            throw e;
        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            throw new RuntimeException("Failed to create HumanBeing: " + e.getMessage(), e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public HumanBeing updateHumanBeing(HumanBeing humanBeing) throws ValidationException, EntityNotFoundException {
        LOGGER.log(Level.INFO, "Updating HumanBeing with ID: {0}", humanBeing.getId());
        
        try {
            if (humanBeing.getId() == null || !humanBeingDao.existsById(humanBeing.getId())) {
                sessionContext.setRollbackOnly();
                throw new EntityNotFoundException("HumanBeing with ID " + humanBeing.getId() + " not found");
            }
            
            HumanBeing existing = humanBeingDao.findById(humanBeing.getId()).orElseThrow(
                () -> {
                    sessionContext.setRollbackOnly();
                    return new EntityNotFoundException("HumanBeing with ID " + humanBeing.getId() + " not found");
                }
            );
            if (humanBeing.getCreationDate() == null) {
                humanBeing.setCreationDate(existing.getCreationDate());
            }

            applyMachineGunDefault(humanBeing);
            
            validateHumanBeing(humanBeing);
            validateBusinessRules(humanBeing, true, humanBeing.getId());

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
        } catch (ValidationException | EntityNotFoundException e) {
            sessionContext.setRollbackOnly();
            throw e;
        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            throw new RuntimeException("Failed to update HumanBeing: " + e.getMessage(), e);
        }
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

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean deleteHumanBeing(Long id) throws EntityNotFoundException {
        LOGGER.log(Level.INFO, "Deleting HumanBeing with ID: {0}", id);
        
        try {
            Optional<HumanBeing> humanBeing = humanBeingDao.findById(id);
            if (humanBeing.isEmpty()) {
                sessionContext.setRollbackOnly();
                throw new EntityNotFoundException("HumanBeing with ID " + id + " not found");
            }
            
            boolean deleted = humanBeingDao.deleteById(id);
            
            if (deleted) {
                LOGGER.log(Level.INFO, "Successfully deleted HumanBeing with ID: {0}", id);
            }
            
            return deleted;
        } catch (EntityNotFoundException e) {
            sessionContext.setRollbackOnly();
            throw e;
        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            throw new RuntimeException("Failed to delete HumanBeing: " + e.getMessage(), e);
        }
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

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int deleteHeroesWithoutToothpicks() {
        LOGGER.log(Level.INFO, "Deleting all heroes without toothpicks");
        try {
            return humanBeingDao.deleteHeroesWithoutToothpicks();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting heroes without toothpicks", e);
            sessionContext.setRollbackOnly(); 
            throw new RuntimeException("Failed to delete heroes without toothpicks: " + e.getMessage(), e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int setAllMoodToSadness() {
        LOGGER.log(Level.INFO, "Setting all heroes mood to SADNESS");
        try {
            return humanBeingDao.setAllMoodToSadness();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting all mood to sadness", e);
            sessionContext.setRollbackOnly(); 
            throw new RuntimeException("Failed to set all mood to sadness: " + e.getMessage(), e);
        }
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

    private void applyMachineGunDefault(HumanBeing humanBeing) {
        if (humanBeing.getWeaponType() == WeaponType.MACHINE_GUN && humanBeing.getImpactSpeed() == 0) {
            humanBeing.setImpactSpeed(20.0f);
            LOGGER.log(Level.INFO, "Applied default impactSpeed=20 for MACHINE_GUN");
        }
    }


    private void validateUniqueCoordinates(HumanBeing humanBeing, boolean isUpdate, Long excludeId, StringBuilder errors) {
        if (humanBeing.getCoordinates() == null || humanBeing.getCoordinates().getX() == null) {
            return; 
        }
        
        boolean shouldCheck = true;
        
        if (isUpdate && excludeId != null) {
            HumanBeing existing = humanBeingDao.findById(excludeId).orElse(null);
            if (existing != null && existing.getCoordinates() != null) {
                Integer existingX = existing.getCoordinates().getX();
                double existingY = existing.getCoordinates().getY();
                
                if (existingX.equals(humanBeing.getCoordinates().getX()) && 
                    existingY == humanBeing.getCoordinates().getY()) {
                    shouldCheck = false;
                }
            }
        }
        
        if (shouldCheck) {
            Optional<HumanBeing> existing = humanBeingDao.findByCoordinates(
                humanBeing.getCoordinates().getX(),
                humanBeing.getCoordinates().getY(),
                excludeId
            );
            
            if (existing.isPresent()) {
                errors.append("HumanBeing with coordinates (" + 
                    humanBeing.getCoordinates().getX() + ", " + 
                    humanBeing.getCoordinates().getY() + 
                    ") already exists; ");
            }
        }
    }

    private void validateMachineGunRule(HumanBeing humanBeing, boolean isUpdate, Long excludeId, StringBuilder errors) {
        if (humanBeing.getWeaponType() != WeaponType.MACHINE_GUN) {
            return;
        }
        
        boolean shouldCheck = true;
        
        if (isUpdate && excludeId != null) {
            HumanBeing existing = humanBeingDao.findById(excludeId).orElse(null);
            if (existing != null) {
                if (existing.getWeaponType() == WeaponType.MACHINE_GUN &&
                    existing.getImpactSpeed() == humanBeing.getImpactSpeed()) {
                    shouldCheck = false;
                }
            }
        }
        
        if (shouldCheck && humanBeing.getImpactSpeed() < 20) {
            errors.append("MACHINE_GUN requires impactSpeed >= 20 (current: " + 
                humanBeing.getImpactSpeed() + "); ");
        }
    }


    private void validateBusinessRules(HumanBeing humanBeing, boolean isUpdate, Long excludeId) throws ValidationException {
        StringBuilder errors = new StringBuilder();
        
        validateUniqueCoordinates(humanBeing, isUpdate, excludeId, errors);

        validateMachineGunRule(humanBeing, isUpdate, excludeId, errors);
        
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

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ImportResult importHumanBeings(List<HumanBeingDto> humanBeingDtos) {
        LOGGER.log(Level.INFO, "Starting import of {0} HumanBeings", humanBeingDtos.size());
        
        List<String> errors = new ArrayList<>();
        int successfullyImported = 0;
        int failed = 0;
        
        try {
            for (int i = 0; i < humanBeingDtos.size(); i++) {
                HumanBeingDto dto = humanBeingDtos.get(i);
                try {
                    validateImportData(dto, i + 1);
                } catch (ValidationException e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                    failed++;
                }
            }
            
            if (!errors.isEmpty()) {
                sessionContext.setRollbackOnly();
                return ImportResult.failure("Validation failed - no objects imported", humanBeingDtos.size(), 
                                          0, failed, errors);
            }

            List<HumanBeing> entitiesToCreate = new ArrayList<>();

            for (int i = 0; i < humanBeingDtos.size(); i++) {
                HumanBeingDto dto = humanBeingDtos.get(i);
                HumanBeing humanBeing = mapper.toEntity(dto);

                if (humanBeing.getCreationDate() == null) {
                    humanBeing.setCreationDate(new java.util.Date());
                }

                applyMachineGunDefault(humanBeing);

                if (humanBeing.getCar() != null && humanBeing.getCar().getId() == null) {
                    Car savedCar = carDao.create(humanBeing.getCar());
                    humanBeing.setCar(savedCar);
                }
                
                entitiesToCreate.add(humanBeing);
            }

            for (int i = 0; i < entitiesToCreate.size(); i++) {
                HumanBeing current = entitiesToCreate.get(i);
                if (current.getCoordinates() != null && current.getCoordinates().getX() != null) {
                    for (int j = i + 1; j < entitiesToCreate.size(); j++) {
                        HumanBeing other = entitiesToCreate.get(j);
                        if (other.getCoordinates() != null && other.getCoordinates().getX() != null) {
                            if (current.getCoordinates().getX().equals(other.getCoordinates().getX()) &&
                                current.getCoordinates().getY() == other.getCoordinates().getY()) {
                                throw new ValidationException(
                                    "Row " + (i + 1) + " and Row " + (j + 1) + 
                                    " have duplicate coordinates (" + 
                                    current.getCoordinates().getX() + ", " + 
                                    current.getCoordinates().getY() + ")"
                                );
                            }
                        }
                    }
                }

                if (current.getCoordinates() != null && current.getCoordinates().getX() != null) {
                    Optional<HumanBeing> existing = humanBeingDao.findByCoordinates(
                        current.getCoordinates().getX(),
                        current.getCoordinates().getY(),
                        null
                    );
                    if (existing.isPresent()) {
                        throw new ValidationException(
                            "Row " + (i + 1) + ": HumanBeing with coordinates (" + 
                            current.getCoordinates().getX() + ", " + 
                            current.getCoordinates().getY() + ") already exists in database"
                        );
                    }
                }
            }

            for (HumanBeing entity : entitiesToCreate) {
                humanBeingDao.create(entity);
                successfullyImported++;
            }
            
            LOGGER.log(Level.INFO, "Import completed successfully: {0} objects imported", successfullyImported);
            
            return ImportResult.success(humanBeingDtos.size(), successfullyImported);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Import failed with exception - all changes rolled back", e);
            sessionContext.setRollbackOnly(); 
            return ImportResult.failure("Import failed - no objects imported: " + e.getMessage(), 
                                      humanBeingDtos.size(), 0, humanBeingDtos.size(), errors);
        }
    }
    
    private void validateImportData(HumanBeingDto dto, int rowNumber) throws ValidationException {
        StringBuilder errors = new StringBuilder();
        
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            errors.append("Name is required; ");
        }
        
        if (dto.getCoordinates() == null) {
            errors.append("Coordinates are required; ");
        } else {
            if (dto.getCoordinates().getX() == null) {
                errors.append("Coordinates X is required; ");
            }
        }
        
        if (dto.getCar() == null) {
            errors.append("Car is required; ");
        } else {
            if (dto.getCar().getName() == null || dto.getCar().getName().trim().isEmpty()) {
                errors.append("Car name is required; ");
            }
        }
        
    
        if (dto.getMood() == null || dto.getMood().trim().isEmpty()) {
            errors.append("Mood is required; ");
        } else {
            try {
                Mood.valueOf(dto.getMood().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.append("Invalid mood value. Must be one of: SORROW, LONGING, GLOOM, APATHY, FRENZY; ");
            }
        }
        
        WeaponType weaponType = null;
        if (dto.getWeaponType() == null || dto.getWeaponType().trim().isEmpty()) {
            errors.append("Weapon type is required; ");
        } else {
            try {
                weaponType = WeaponType.valueOf(dto.getWeaponType().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.append("Invalid weapon type value; ");
            }
        }

        if (weaponType == WeaponType.MACHINE_GUN && dto.getImpactSpeed() < 20) {
            errors.append("MACHINE_GUN requires impactSpeed >= 20 (current: " + dto.getImpactSpeed() + "); ");
        }
        
        if (dto.getSoundtrackName() == null || dto.getSoundtrackName().trim().isEmpty()) {
            errors.append("Soundtrack name is required; ");
        }
        
        if (dto.getMinutesOfWaiting() == null) {
            errors.append("Minutes of waiting is required; ");
        }

        if (dto.getName() != null && dto.getName().length() > 100) {
            errors.append("Name must be 100 characters or less; ");
        }
        
        if (dto.getSoundtrackName() != null && dto.getSoundtrackName().length() > 100) {
            errors.append("Soundtrack name must be 100 characters or less; ");
        }
        
        if (dto.getCar() != null && dto.getCar().getName() != null && dto.getCar().getName().length() > 50) {
            errors.append("Car name must be 50 characters or less; ");
        }
        
        if (!errors.toString().isEmpty()) {
            throw new ValidationException(errors.toString());
        }
    }
}
