package com.humanbeingmanager.validator;

import com.humanbeingmanager.dao.HumanBeingDao;
import com.humanbeingmanager.entity.HumanBeing;
import com.humanbeingmanager.entity.Car;
import com.humanbeingmanager.entity.Coordinates;
import com.humanbeingmanager.entity.WeaponType;
import com.humanbeingmanager.dto.CoordinatesDto;
import com.humanbeingmanager.dto.CarDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ejb.EJB;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

@ApplicationScoped
public class BusinessRulesValidator {

    private static final Logger LOGGER = Logger.getLogger(BusinessRulesValidator.class.getName());

    @EJB
    private HumanBeingDao humanBeingDao;


    public void applyMachineGunDefault(HumanBeing humanBeing) {
        if (humanBeing.getWeaponType() == WeaponType.MACHINE_GUN && humanBeing.getImpactSpeed() == 0) {
            humanBeing.setImpactSpeed(20.0f);
            LOGGER.log(Level.INFO, "Applied default impactSpeed=20 for MACHINE_GUN");
        }
    }

    public void validateMachineGunRule(HumanBeing humanBeing, StringBuilder errors) {
        if (humanBeing.getWeaponType() == WeaponType.MACHINE_GUN && humanBeing.getImpactSpeed() < 20) {
            errors.append("MACHINE_GUN requires impactSpeed >= 20 (current: ")
                  .append(humanBeing.getImpactSpeed()).append("); ");
        }
    }

    
    public void validateMachineGunRule(HumanBeing humanBeing, boolean isUpdate, Long excludeId, StringBuilder errors) {
        if (humanBeing.getWeaponType() != WeaponType.MACHINE_GUN) {
            return;
        }
        
        boolean shouldCheck = true;
        
        if (isUpdate && excludeId != null) {
            Optional<HumanBeing> existingOpt = humanBeingDao.findById(excludeId);
            if (existingOpt.isPresent()) {
                HumanBeing existing = existingOpt.get();
                if (existing.getWeaponType() == WeaponType.MACHINE_GUN &&
                    existing.getImpactSpeed() == humanBeing.getImpactSpeed()) {
                    shouldCheck = false;
                }
            }
        }
        
        if (shouldCheck) {
            validateMachineGunRule(humanBeing, errors);
        }
    }

    
    public void validateMachineGunRule(WeaponType weaponType, float impactSpeed, StringBuilder errors) {
        if (weaponType == WeaponType.MACHINE_GUN && impactSpeed < 20) {
            errors.append("MACHINE_GUN requires impactSpeed >= 20 (current: ")
                  .append(impactSpeed).append("); ");
        }
    }

    /
    public void validateName(String name, StringBuilder errors) {
        if (name != null) {
            String trimmed = name.trim();
            if (trimmed.length() > 100) {
                errors.append("Name must be 100 characters or less; ");
            }
            if (!trimmed.matches("^[a-zA-Z0-9\\s\\-_.]+$")) {
                errors.append("Name can only contain letters, numbers, spaces, hyphens, underscores, and periods; ");
            }
        }
    }

    
    public void validateCoordinates(Coordinates coordinates, StringBuilder errors) {
        if (coordinates != null) {
            if (coordinates.getX() != null) {
                if (coordinates.getX() < -1000 || coordinates.getX() > 1000) {
                    errors.append("X coordinate must be between -1000 and 1000; ");
                }
                if (coordinates.getX() == 0) {
                    errors.append("X coordinate cannot be zero; ");
                }
            }
            
            if (coordinates.getY() <= -1000) {
                errors.append("Y coordinate must be greater than -1000; ");
            }
            if (coordinates.getY() > 1000) {
                errors.append("Y coordinate must be at most 1000; ");
            }
            if (coordinates.getY() == 0) {
                errors.append("Y coordinate cannot be zero; ");
            }
        }
    }

    
    public void validateCoordinates(CoordinatesDto coordinates, StringBuilder errors) {
        if (coordinates != null) {
            if (coordinates.getX() != null) {
                if (coordinates.getX() < -1000 || coordinates.getX() > 1000) {
                    errors.append("X coordinate must be between -1000 and 1000; ");
                }
                if (coordinates.getX() == 0) {
                    errors.append("X coordinate cannot be zero; ");
                }
            }
            
            if (coordinates.getY() <= -1000) {
                errors.append("Y coordinate must be greater than -1000; ");
            }
            if (coordinates.getY() > 1000) {
                errors.append("Y coordinate must be at most 1000; ");
            }
            if (coordinates.getY() == 0) {
                errors.append("Y coordinate cannot be zero; ");
            }
        }
    }

    
    public void validateImpactSpeed(HumanBeing humanBeing, StringBuilder errors) {
        if (humanBeing.isRealHero() && humanBeing.getImpactSpeed() < 0) {
            errors.append("Real heroes cannot have negative impact speed; ");
        }
        if (humanBeing.getImpactSpeed() < -1000 || humanBeing.getImpactSpeed() > 1000) {
            errors.append("Impact speed must be between -1000 and 1000; ");
        }
    }

    
    public void validateMinutesOfWaiting(Long minutesOfWaiting, StringBuilder errors) {
        if (minutesOfWaiting != null) {
            if (minutesOfWaiting < 0) {
                errors.append("Minutes of waiting cannot be negative; ");
            }
            if (minutesOfWaiting > 99999) {
                errors.append("Minutes of waiting must be less than 100,000; ");
            }
        }
    }

    
    public void validateSoundtrackName(String soundtrackName, StringBuilder errors) {
        if (soundtrackName != null) {
            String trimmed = soundtrackName.trim();
            if (trimmed.length() > 100) {
                errors.append("Soundtrack name must be 100 characters or less; ");
            }
            if (!trimmed.matches("^[a-zA-Z0-9\\s\\-_.]+$")) {
                errors.append("Soundtrack name can only contain letters, numbers, spaces, hyphens, underscores, and periods; ");
            }
        }
    }

    
    public void validateCar(Car car, StringBuilder errors) {
        if (car != null) {
            if (car.getName() != null) {
                if (car.getName().length() > 50) {
                    errors.append("Car name must be 50 characters or less; ");
                }
                if (!car.getName().matches("^[a-zA-Z0-9\\s\\-_.]*$")) {
                    errors.append("Car name can only contain letters, numbers, spaces, hyphens, underscores, and periods; ");
                }
            }
        }
    }

    public void validateCar(CarDto car, StringBuilder errors) {
        if (car != null) {
            if (car.getName() != null) {
                if (car.getName().length() > 50) {
                    errors.append("Car name must be 50 characters or less; ");
                }
                if (!car.getName().matches("^[a-zA-Z0-9\\s\\-_.]*$")) {
                    errors.append("Car name can only contain letters, numbers, spaces, hyphens, underscores, and periods; ");
                }
            }
        }
    }


    public boolean coordinatesExist(Integer x, double y) {
        if (x == null) {
            return false;
        }
        Optional<HumanBeing> existing = humanBeingDao.findByCoordinates(x, y, null);
        return existing.isPresent();
    }

    
    public void validateUniqueCoordinates(HumanBeing humanBeing, boolean isUpdate, Long excludeId, StringBuilder errors) {
        if (humanBeing.getCoordinates() == null || humanBeing.getCoordinates().getX() == null) {
            return; 
        }
        
        boolean shouldCheck = true;
        
        if (isUpdate && excludeId != null) {
            Optional<HumanBeing> existingOpt = humanBeingDao.findById(excludeId);
            if (existingOpt.isPresent()) {
                HumanBeing existing = existingOpt.get();
                if (existing.getCoordinates() != null) {
                    Integer existingX = existing.getCoordinates().getX();
                    double existingY = existing.getCoordinates().getY();
                    
                    if (existingX.equals(humanBeing.getCoordinates().getX()) && 
                        existingY == humanBeing.getCoordinates().getY()) {
                        shouldCheck = false;
                    }
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

    public void validateBusinessRules(HumanBeing humanBeing, StringBuilder errors) {
        validateName(humanBeing.getName(), errors);
        validateCoordinates(humanBeing.getCoordinates(), errors);
        validateImpactSpeed(humanBeing, errors);
        validateMinutesOfWaiting(humanBeing.getMinutesOfWaiting(), errors);
        validateSoundtrackName(humanBeing.getSoundtrackName(), errors);
        validateCar(humanBeing.getCar(), errors);
        validateMachineGunRule(humanBeing, errors);
    }
}

