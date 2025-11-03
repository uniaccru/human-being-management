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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;
import java.util.ArrayList;
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

    @Inject
    private EntityManager entityManager;

    @Inject
    private EntityDtoMapper mapper;

    public HumanBeing createHumanBeing(HumanBeing humanBeing) throws ValidationException {
        LOGGER.log(Level.INFO, "Creating new HumanBeing: {0}", humanBeing.getName());
        
        // Управляем транзакцией вручную для обеспечения атомарности и работы блокировки
        EntityTransaction transaction = entityManager.getTransaction();
        boolean transactionStartedHere = false;
        
        try {
            // Начинаем транзакцию если её нет
            if (!transaction.isActive()) {
                transaction.begin();
                transactionStartedHere = true;
            }
            
            // Устанавливаем creationDate автоматически при создании
            if (humanBeing.getCreationDate() == null) {
                humanBeing.setCreationDate(new java.util.Date());
            }
            
            // Обработка MACHINE_GUN: устанавливаем impactSpeed = 20 если не заполнено
            applyMachineGunDefault(humanBeing);
            
            validateHumanBeing(humanBeing);
            validateBusinessRules(humanBeing, false, null);
            
            if (humanBeing.getCar() != null && humanBeing.getCar().getId() == null) {
                Car savedCar = carDao.create(humanBeing.getCar());
                humanBeing.setCar(savedCar);
            }

            HumanBeing created = humanBeingDao.create(humanBeing);
            
            // Коммитим транзакцию если мы её начали
            if (transactionStartedHere && transaction.isActive()) {
                transaction.commit();
            }
            
            LOGGER.log(Level.INFO, "Successfully created HumanBeing with ID: {0}", created.getId());
            
            return created;
        } catch (Exception e) {
            // Откатываем транзакцию если мы её начали
            if (transactionStartedHere && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public HumanBeing updateHumanBeing(HumanBeing humanBeing) throws ValidationException, EntityNotFoundException {
        LOGGER.log(Level.INFO, "Updating HumanBeing with ID: {0}", humanBeing.getId());
        
        if (humanBeing.getId() == null || !humanBeingDao.existsById(humanBeing.getId())) {
            throw new EntityNotFoundException("HumanBeing with ID " + humanBeing.getId() + " not found");
        }
        
        // Сохраняем существующий creationDate
        HumanBeing existing = humanBeingDao.findById(humanBeing.getId()).orElseThrow(
            () -> new EntityNotFoundException("HumanBeing with ID " + humanBeing.getId() + " not found")
        );
        if (humanBeing.getCreationDate() == null) {
            humanBeing.setCreationDate(existing.getCreationDate());
        }
        
        // Обработка MACHINE_GUN: устанавливаем impactSpeed = 20 если не заполнено
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

    /**
     * Применяет дефолтное значение impactSpeed для MACHINE_GUN
     */
    private void applyMachineGunDefault(HumanBeing humanBeing) {
        if (humanBeing.getWeaponType() == WeaponType.MACHINE_GUN && humanBeing.getImpactSpeed() == 0) {
            // Если impactSpeed не заполнено (равно 0), устанавливаем дефолт 20
            humanBeing.setImpactSpeed(20.0f);
            LOGGER.log(Level.INFO, "Applied default impactSpeed=20 for MACHINE_GUN");
        }
    }

    /**
     * Проверяет уникальность координат
     */
    private void validateUniqueCoordinates(HumanBeing humanBeing, boolean isUpdate, Long excludeId, StringBuilder errors) {
        if (humanBeing.getCoordinates() == null || humanBeing.getCoordinates().getX() == null) {
            return; // Координаты уже проверяются в других правилах
        }
        
        boolean shouldCheck = true;
        
        if (isUpdate && excludeId != null) {
            // При UPDATE проверяем только если координаты изменились
            HumanBeing existing = humanBeingDao.findById(excludeId).orElse(null);
            if (existing != null && existing.getCoordinates() != null) {
                Integer existingX = existing.getCoordinates().getX();
                double existingY = existing.getCoordinates().getY();
                
                // Если координаты не изменились, не проверяем уникальность
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

    /**
     * Проверяет правило для MACHINE_GUN: impactSpeed >= 20
     */
    private void validateMachineGunRule(HumanBeing humanBeing, boolean isUpdate, Long excludeId, StringBuilder errors) {
        if (humanBeing.getWeaponType() != WeaponType.MACHINE_GUN) {
            return;
        }
        
        boolean shouldCheck = true;
        
        if (isUpdate && excludeId != null) {
            // При UPDATE проверяем только если weaponType или impactSpeed изменились
            HumanBeing existing = humanBeingDao.findById(excludeId).orElse(null);
            if (existing != null) {
                // Если weaponType был MACHINE_GUN и остался, и impactSpeed не изменился - не проверяем
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

    /**
     * Валидирует бизнес-правила
     * @param humanBeing объект для валидации
     * @param isUpdate true если это обновление, false если создание
     * @param excludeId ID объекта, который нужно исключить из проверки (для UPDATE)
     */
    private void validateBusinessRules(HumanBeing humanBeing, boolean isUpdate, Long excludeId) throws ValidationException {
        StringBuilder errors = new StringBuilder();
        
        // Проверка уникальности координат
        validateUniqueCoordinates(humanBeing, isUpdate, excludeId, errors);
        
        // Проверка правила для MACHINE_GUN
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

    public ImportResult importHumanBeings(List<HumanBeingDto> humanBeingDtos) {
        LOGGER.log(Level.INFO, "Starting import of {0} HumanBeings", humanBeingDtos.size());
        
        // Управляем транзакцией вручную для обеспечения атомарности импорта
        EntityTransaction transaction = entityManager.getTransaction();
        boolean transactionStartedHere = false;
        
        List<String> errors = new ArrayList<>();
        int successfullyImported = 0;
        int failed = 0;
        
        try {
            // Начинаем транзакцию если её нет
            if (!transaction.isActive()) {
                transaction.begin();
                transactionStartedHere = true;
            }
            // Предварительная валидация всех объектов
            for (int i = 0; i < humanBeingDtos.size(); i++) {
                HumanBeingDto dto = humanBeingDtos.get(i);
                try {
                    validateImportData(dto, i + 1);
                } catch (ValidationException e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                    failed++;
                }
            }
            
            // Если есть ошибки валидации, возвращаем ошибку БЕЗ импорта
            if (!errors.isEmpty()) {
                // Откатываем транзакцию если мы её начали
                if (transactionStartedHere && transaction.isActive()) {
                    transaction.rollback();
                }
                return ImportResult.failure("Validation failed - no objects imported", humanBeingDtos.size(), 
                                          0, failed, errors);
            }
            
            // Импортируем все объекты в одной транзакции
            // Если любой объект не удастся импортировать, откатываем все
            List<HumanBeing> entitiesToCreate = new ArrayList<>();
            
            // Подготавливаем все объекты для создания
            for (int i = 0; i < humanBeingDtos.size(); i++) {
                HumanBeingDto dto = humanBeingDtos.get(i);
                HumanBeing humanBeing = mapper.toEntity(dto);
                
                // Устанавливаем creationDate автоматически
                if (humanBeing.getCreationDate() == null) {
                    humanBeing.setCreationDate(new java.util.Date());
                }
                
                // Обработка MACHINE_GUN: устанавливаем impactSpeed = 20 если не заполнено
                applyMachineGunDefault(humanBeing);
                
                // Сохраняем Car вручную перед созданием HumanBeing (каскад убран)
                if (humanBeing.getCar() != null && humanBeing.getCar().getId() == null) {
                    Car savedCar = carDao.create(humanBeing.getCar());
                    humanBeing.setCar(savedCar);
                }
                
                entitiesToCreate.add(humanBeing);
            }
            
            // Проверка уникальности координат внутри импортируемых объектов
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
                
                // Проверяем уникальность с существующими в БД
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
            
            // Создаем все объекты
            for (HumanBeing entity : entitiesToCreate) {
                humanBeingDao.create(entity);
                successfullyImported++;
            }
            
            // Коммитим транзакцию если мы её начали
            if (transactionStartedHere && transaction.isActive()) {
                transaction.commit();
            }
            
            LOGGER.log(Level.INFO, "Import completed successfully: {0} objects imported", successfullyImported);
            
            return ImportResult.success(humanBeingDtos.size(), successfullyImported);
            
        } catch (Exception e) {
            // Откатываем транзакцию если мы её начали
            if (transactionStartedHere && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Import failed with exception - all changes rolled back", e);
            return ImportResult.failure("Import failed - no objects imported: " + e.getMessage(), 
                                      humanBeingDtos.size(), 0, humanBeingDtos.size(), errors);
        }
    }
    
    private void validateImportData(HumanBeingDto dto, int rowNumber) throws ValidationException {
        StringBuilder errors = new StringBuilder();
        
        // Проверяем обязательные поля
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
        
        // Проверяем Mood enum
        if (dto.getMood() == null || dto.getMood().trim().isEmpty()) {
            errors.append("Mood is required; ");
        } else {
            try {
                Mood.valueOf(dto.getMood().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.append("Invalid mood value. Must be one of: SORROW, LONGING, GLOOM, APATHY, FRENZY; ");
            }
        }
        
        // Проверяем WeaponType enum
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
        
        // Проверяем правило для MACHINE_GUN
        if (weaponType == WeaponType.MACHINE_GUN && dto.getImpactSpeed() < 20) {
            errors.append("MACHINE_GUN requires impactSpeed >= 20 (current: " + dto.getImpactSpeed() + "); ");
        }
        
        if (dto.getSoundtrackName() == null || dto.getSoundtrackName().trim().isEmpty()) {
            errors.append("Soundtrack name is required; ");
        }
        
        if (dto.getMinutesOfWaiting() == null) {
            errors.append("Minutes of waiting is required; ");
        }
        
        // Проверяем ограничения длины
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
