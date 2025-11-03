package com.humanbeingmanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Date;

public class HumanBeingDto {
    
    private Long id;
    
    @NotNull(message = "Name cannot be null")
    @NotBlank(message = "Name cannot be empty")
    @Size(max = 100, message = "Name must be 100 characters or less")
    private String name;
    
    @Valid
    @NotNull(message = "Coordinates cannot be null")
    private CoordinatesDto coordinates;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date creationDate;
    
    private boolean realHero;
    
    private Boolean hasToothpick;
    
    @Valid
    @NotNull(message = "Car cannot be null")
    private CarDto car;
    
    @NotNull(message = "Mood cannot be null")
    private String mood;
    
    private float impactSpeed;
    
    @NotNull(message = "Soundtrack name cannot be null")
    @NotBlank(message = "Soundtrack name cannot be empty")
    @Size(max = 100, message = "Soundtrack name must be 100 characters or less")
    private String soundtrackName;
    
    @NotNull(message = "Minutes of waiting cannot be null")
    private Long minutesOfWaiting;
    
    @NotNull(message = "Weapon type cannot be null")
    private String weaponType;

    public HumanBeingDto() {}

    public HumanBeingDto(Long id, String name, CoordinatesDto coordinates, Date creationDate, 
                        boolean realHero, Boolean hasToothpick, CarDto car, String mood, 
                        float impactSpeed, String soundtrackName, Long minutesOfWaiting, 
                        String weaponType) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = creationDate;
        this.realHero = realHero;
        this.hasToothpick = hasToothpick;
        this.car = car;
        this.mood = mood;
        this.impactSpeed = impactSpeed;
        this.soundtrackName = soundtrackName;
        this.minutesOfWaiting = minutesOfWaiting;
        this.weaponType = weaponType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CoordinatesDto getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(CoordinatesDto coordinates) {
        this.coordinates = coordinates;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isRealHero() {
        return realHero;
    }

    public void setRealHero(boolean realHero) {
        this.realHero = realHero;
    }

    public Boolean getHasToothpick() {
        return hasToothpick;
    }

    public void setHasToothpick(Boolean hasToothpick) {
        this.hasToothpick = hasToothpick;
    }

    public CarDto getCar() {
        return car;
    }

    public void setCar(CarDto car) {
        this.car = car;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public float getImpactSpeed() {
        return impactSpeed;
    }

    public void setImpactSpeed(float impactSpeed) {
        this.impactSpeed = impactSpeed;
    }

    public String getSoundtrackName() {
        return soundtrackName;
    }

    public void setSoundtrackName(String soundtrackName) {
        this.soundtrackName = soundtrackName;
    }

    public Long getMinutesOfWaiting() {
        return minutesOfWaiting;
    }

    public void setMinutesOfWaiting(Long minutesOfWaiting) {
        this.minutesOfWaiting = minutesOfWaiting;
    }

    public String getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(String weaponType) {
        this.weaponType = weaponType;
    }

    @Override
    public String toString() {
        return "HumanBeingDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", creationDate=" + creationDate +
                ", realHero=" + realHero +
                ", hasToothpick=" + hasToothpick +
                ", car=" + car +
                ", mood='" + mood + '\'' +
                ", impactSpeed=" + impactSpeed +
                ", soundtrackName='" + soundtrackName + '\'' +
                ", minutesOfWaiting=" + minutesOfWaiting +
                ", weaponType='" + weaponType + '\'' +
                '}';
    }
}


