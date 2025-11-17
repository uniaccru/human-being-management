package com.humanbeingmanager.service;

import com.humanbeingmanager.dao.HumanBeingDao;
import com.humanbeingmanager.entity.HumanBeing;
import com.humanbeingmanager.entity.Mood;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.SessionContext;
import jakarta.ejb.EJB;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SpecialOperationsService {

    private static final Logger LOGGER = Logger.getLogger(SpecialOperationsService.class.getName());

    @EJB
    private HumanBeingDao humanBeingDao;

    @Resource
    private SessionContext sessionContext;

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
            // Получаем список героев без зубочисток из DAO
            List<HumanBeing> toDelete = humanBeingDao.findHeroesWithoutToothpicks();
            
            // Бизнес-логика: удаляем каждого героя
            int deletedCount = 0;
            for (HumanBeing humanBeing : toDelete) {
                humanBeingDao.delete(humanBeing);
                deletedCount++;
            }
            
            LOGGER.log(Level.INFO, "Successfully deleted {0} heroes without toothpicks", deletedCount);
            return deletedCount;
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
            // Получаем список всех героев из DAO
            List<HumanBeing> heroes = humanBeingDao.findAllRealHeroes();
            
            // Бизнес-логика: устанавливаем настроение SADNESS для каждого героя
            int updatedCount = 0;
            for (HumanBeing hero : heroes) {
                hero.setMood(Mood.SADNESS);
                humanBeingDao.update(hero);
                updatedCount++;
            }
            
            LOGGER.log(Level.INFO, "Successfully updated mood to SADNESS for {0} heroes", updatedCount);
            return updatedCount;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting all mood to sadness", e);
            sessionContext.setRollbackOnly(); 
            throw new RuntimeException("Failed to set all mood to sadness: " + e.getMessage(), e);
        }
    }
}

