package com.humanbeingmanager.service;

import com.humanbeingmanager.dao.HumanBeingDao;
import com.humanbeingmanager.entity.HumanBeing;
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
}

