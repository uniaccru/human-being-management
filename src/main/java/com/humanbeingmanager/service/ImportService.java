package com.humanbeingmanager.service;

import com.humanbeingmanager.dao.HumanBeingDao;
import com.humanbeingmanager.dao.CarDao;
import com.humanbeingmanager.entity.HumanBeing;
import com.humanbeingmanager.entity.Car;
import com.humanbeingmanager.entity.Mood;
import com.humanbeingmanager.entity.WeaponType;
import com.humanbeingmanager.dto.HumanBeingDto;
import com.humanbeingmanager.dto.ImportResultDto;
import com.humanbeingmanager.mapper.EntityDtoMapper;
import com.humanbeingmanager.validator.BusinessRulesValidator;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.SessionContext;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ImportService {

    private static final Logger LOGGER = Logger.getLogger(ImportService.class.getName());

    @EJB
    private HumanBeingDao humanBeingDao;

    @EJB
    private CarDao carDao;

    @Inject
    private EntityDtoMapper mapper;

    @Inject
    private BusinessRulesValidator businessRulesValidator;

    @Resource
    private SessionContext sessionContext;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ImportResultDto importHumanBeings(List<HumanBeingDto> humanBeingDtos) {
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
                return ImportResultDto.failure("Validation failed - no objects imported", humanBeingDtos.size(), 
                                          0, failed, errors);
            }

            List<HumanBeing> entitiesToCreate = new ArrayList<>();

            for (int i = 0; i < humanBeingDtos.size(); i++) {
                HumanBeingDto dto = humanBeingDtos.get(i);
                HumanBeing humanBeing = mapper.toEntity(dto);

                if (humanBeing.getCreationDate() == null) {
                    humanBeing.setCreationDate(new java.util.Date());
                }

                businessRulesValidator.applyMachineGunDefault(humanBeing);

                if (humanBeing.getCar() != null && humanBeing.getCar().getId() == null) {
                    Car savedCar = carDao.create(humanBeing.getCar());
                    humanBeing.setCar(savedCar);
                }
                
                entitiesToCreate.add(humanBeing);
            }

            //в рамках файла
            for (int i = 0; i < entitiesToCreate.size(); i++) {
                HumanBeing current = entitiesToCreate.get(i);
                int rowNumber = i + 1;
                
                //в рмках файла
                String duplicateError = businessRulesValidator.validateUniqueCoordinatesInImportList(
                    entitiesToCreate, rowNumber
                );
                if (duplicateError != null) {
                    throw new ValidationException(duplicateError);
                }
                
                // в бд
                StringBuilder coordinateErrors = new StringBuilder();
                businessRulesValidator.validateUniqueCoordinates(current, false, null, coordinateErrors);
                if (coordinateErrors.length() > 0) {
                    throw new ValidationException("Row " + rowNumber + ": " + coordinateErrors.toString().trim());
                }
            }

            for (HumanBeing entity : entitiesToCreate) {
                humanBeingDao.create(entity);
                successfullyImported++;
            }
            
            LOGGER.log(Level.INFO, "Import completed successfully: {0} objects imported", successfullyImported);
            
            return ImportResultDto.success(humanBeingDtos.size(), successfullyImported);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Import failed with exception - all changes rolled back", e);
            sessionContext.setRollbackOnly(); 
            return ImportResultDto.failure("Import failed - no objects imported: " + e.getMessage(), 
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
                errors.append("Invalid mood value.");
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

        if (weaponType != null) {
            businessRulesValidator.validateMachineGunRule(weaponType, dto.getImpactSpeed(), errors);
        }
        
        if (dto.getSoundtrackName() == null || dto.getSoundtrackName().trim().isEmpty()) {
            errors.append("Soundtrack name is required; ");
        }
        
        if (dto.getMinutesOfWaiting() == null) {
            errors.append("Minutes of waiting is required; ");
        }

        businessRulesValidator.validateName(dto.getName(), errors);
        businessRulesValidator.validateSoundtrackName(dto.getSoundtrackName(), errors);
        businessRulesValidator.validateCar(dto.getCar(), errors);
        businessRulesValidator.validateMinutesOfWaiting(dto.getMinutesOfWaiting(), errors);
        
        if (dto.getCoordinates() != null) {
            businessRulesValidator.validateCoordinates(dto.getCoordinates(), errors);
        }
        
        if (!errors.toString().isEmpty()) {
            throw new ValidationException(errors.toString());
        }
    }


    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}

