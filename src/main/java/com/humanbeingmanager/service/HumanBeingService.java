package com.humanbeingmanager.service;

import com.humanbeingmanager.dao.HumanBeingDao;
import com.humanbeingmanager.dao.CarDao;
import com.humanbeingmanager.entity.HumanBeing;
import com.humanbeingmanager.entity.Car;
import com.humanbeingmanager.entity.Coordinates;
import com.humanbeingmanager.entity.Mood;
import com.humanbeingmanager.entity.WeaponType;
import com.humanbeingmanager.dto.CarDto;
import com.humanbeingmanager.dto.CoordinatesDto;
import com.humanbeingmanager.mapper.EntityDtoMapper;
import com.humanbeingmanager.validator.BusinessRulesValidator;
import com.humanbeingmanager.config.CacheStatisticsLogging;
import com.humanbeingmanager.exception.ValidationException;
import com.humanbeingmanager.exception.EntityNotFoundException;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.SessionContext;
import jakarta.ejb.EJB;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;

@Stateless
@CacheStatisticsLogging
@Interceptors({com.humanbeingmanager.config.CacheStatisticsInterceptor.class})
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

    @Inject
    private BusinessRulesValidator businessRulesValidator;

    @Inject
    private CoordinateLockManager coordinateLockManager;

    @Resource
    private SessionContext sessionContext;

    /**
     * Validates HumanBeing without persisting. Use for pre-submit validation only.
     * @throws ValidationException if validation or business rules fail
     */
    public void validateHumanBeingOnly(HumanBeing humanBeing) throws ValidationException {
        LOGGER.log(Level.INFO, "Validating HumanBeing (no persist): {0}", humanBeing != null ? humanBeing.getName() : "null");
        if (humanBeing.getCreationDate() == null) {
            humanBeing.setCreationDate(new java.util.Date());
        }
        businessRulesValidator.applyMachineGunDefault(humanBeing);
        validateHumanBeing(humanBeing);
        validateBusinessRules(humanBeing, false, null);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public HumanBeing createHumanBeing(HumanBeing humanBeing) throws ValidationException {
        LOGGER.log(Level.INFO, "Creating new HumanBeing: {0}", humanBeing.getName());
        
        ReentrantLock coordinateLock = null;
        if (humanBeing.getCoordinates() != null && humanBeing.getCoordinates().getX() != null) {
            coordinateLock = coordinateLockManager.getLock(
                humanBeing.getCoordinates().getX(), 
                humanBeing.getCoordinates().getY()
            );
            coordinateLock.lock();
            try {
                return createHumanBeingInternal(humanBeing);
            } finally {
                coordinateLock.unlock();
            }
        } else {
            return createHumanBeingInternal(humanBeing);
        }
    }
    
    private HumanBeing createHumanBeingInternal(HumanBeing humanBeing) throws ValidationException {
        try {
            if (humanBeing.getCreationDate() == null) {
                humanBeing.setCreationDate(new java.util.Date());
            }

            businessRulesValidator.applyMachineGunDefault(humanBeing);
            
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

            businessRulesValidator.applyMachineGunDefault(humanBeing);
            
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


    private void validateBusinessRules(HumanBeing humanBeing, boolean isUpdate, Long excludeId) throws ValidationException {
        StringBuilder errors = new StringBuilder();
        
        businessRulesValidator.validateUniqueCoordinates(humanBeing, isUpdate, excludeId, errors);

        businessRulesValidator.validateMachineGunRule(humanBeing, isUpdate, excludeId, errors);
        
        businessRulesValidator.validateBusinessRules(humanBeing, errors);

        if (errors.length() > 0) {
            throw new ValidationException(errors.toString());
        }
    }

    

}
