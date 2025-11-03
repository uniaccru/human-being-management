package com.humanbeingmanager.mapper;

import com.humanbeingmanager.dto.*;
import com.humanbeingmanager.entity.*;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EntityDtoMapper {

    public HumanBeingDto toDto(HumanBeing entity) {
        if (entity == null) {
            return null;
        }

        HumanBeingDto dto = new HumanBeingDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCoordinates(toDto(entity.getCoordinates()));
        dto.setCreationDate(entity.getCreationDate());
        dto.setRealHero(entity.isRealHero());
        dto.setHasToothpick(entity.getHasToothpick());
        dto.setCar(toDto(entity.getCar()));
        dto.setMood(entity.getMood() != null ? entity.getMood().name() : null);
        dto.setImpactSpeed(entity.getImpactSpeed());
        dto.setSoundtrackName(entity.getSoundtrackName());
        dto.setMinutesOfWaiting(entity.getMinutesOfWaiting());
        dto.setWeaponType(entity.getWeaponType() != null ? entity.getWeaponType().name() : null);

        return dto;
    }

    public HumanBeing toEntity(HumanBeingDto dto) {
        if (dto == null) {
            return null;
        }

        HumanBeing entity = new HumanBeing();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setCoordinates(toEntity(dto.getCoordinates()));
        entity.setCreationDate(dto.getCreationDate());
        entity.setRealHero(dto.isRealHero());
        entity.setHasToothpick(dto.getHasToothpick());
        entity.setCar(toEntity(dto.getCar()));
        entity.setMood(dto.getMood() != null ? Mood.valueOf(dto.getMood()) : null);
        entity.setImpactSpeed(dto.getImpactSpeed());
        entity.setSoundtrackName(dto.getSoundtrackName());
        entity.setMinutesOfWaiting(dto.getMinutesOfWaiting());
        entity.setWeaponType(dto.getWeaponType() != null ? WeaponType.valueOf(dto.getWeaponType()) : null);

        return entity;
    }

    public CarDto toDto(Car entity) {
        if (entity == null) {
            return null;
        }

        CarDto dto = new CarDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCool(entity.isCool());

        return dto;
    }

    public Car toEntity(CarDto dto) {
        if (dto == null) {
            return null;
        }

        Car entity = new Car();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setCool(dto.isCool());

        return entity;
    }

    public CoordinatesDto toDto(Coordinates entity) {
        if (entity == null) {
            return null;
        }

        CoordinatesDto dto = new CoordinatesDto();
        dto.setX(entity.getX());
        dto.setY(entity.getY());

        return dto;
    }

    public Coordinates toEntity(CoordinatesDto dto) {
        if (dto == null) {
            return null;
        }

        Coordinates entity = new Coordinates();
        entity.setX(dto.getX());
        entity.setY(dto.getY());

        return entity;
    }
}


