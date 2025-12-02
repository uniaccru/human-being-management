package com.humanbeingmanager.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Date;

@Entity
@Table(name = "human_beings")
@Cacheable(true)
@NamedQueries({
    @NamedQuery(name = "HumanBeing.findAll", query = "SELECT h FROM HumanBeing h"),
    @NamedQuery(name = "HumanBeing.findById", query = "SELECT h FROM HumanBeing h WHERE h.id = :id"),
    @NamedQuery(name = "HumanBeing.countAll", query = "SELECT COUNT(h) FROM HumanBeing h")
})
public class HumanBeing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    @NotNull(message = "Name cannot be null")
    @NotBlank(message = "Name cannot be empty")
    private String name;
    
    @Embedded
    @Valid
    @NotNull(message = "Coordinates cannot be null")
    private Coordinates coordinates;
    
    @Column(name = "creation_date", nullable = false)
    @NotNull(message = "Creation date cannot be null")
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;
    
    @Column(name = "real_hero", nullable = false)
    private boolean realHero;
    
    @Column(name = "has_toothpick")
    private Boolean hasToothpick;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "car_id", nullable = false)
    @NotNull(message = "Car cannot be null")
    @Valid
    private Car car;
    
    @Column(name = "mood", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Mood cannot be null")
    private Mood mood;
    
    @Column(name = "impact_speed")
    private float impactSpeed;
    
    @Column(name = "soundtrack_name", nullable = false)
    @NotNull(message = "Soundtrack name cannot be null")
    @NotBlank(message = "Soundtrack name cannot be empty")
    private String soundtrackName;
    
    @Column(name = "minutes_of_waiting", nullable = false)
    @NotNull(message = "Minutes of waiting cannot be null")
    private Long minutesOfWaiting;
    
    @Column(name = "weapon_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Weapon type cannot be null")
    private WeaponType weaponType;

    public HumanBeing() {
        this.creationDate = new Date();
    }

    public HumanBeing(String name, Coordinates coordinates, boolean realHero, 
                     Car car, Mood mood, float impactSpeed, String soundtrackName, 
                     Long minutesOfWaiting, WeaponType weaponType) {
        this();
        this.name = name;
        this.coordinates = coordinates;
        this.realHero = realHero;
        this.car = car;
        this.mood = mood;
        this.impactSpeed = impactSpeed;
        this.soundtrackName = soundtrackName;
        this.minutesOfWaiting = minutesOfWaiting;
        this.weaponType = weaponType;
    }

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = new Date();
        }
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

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
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

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Mood getMood() {
        return mood;
    }

    public void setMood(Mood mood) {
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

    public WeaponType getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(WeaponType weaponType) {
        this.weaponType = weaponType;
    }

    @Override
    public String toString() {
        return "HumanBeing{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", creationDate=" + creationDate +
                ", realHero=" + realHero +
                ", hasToothpick=" + hasToothpick +
                ", car=" + car +
                ", mood=" + mood +
                ", impactSpeed=" + impactSpeed +
                ", soundtrackName='" + soundtrackName + '\'' +
                ", minutesOfWaiting=" + minutesOfWaiting +
                ", weaponType=" + weaponType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HumanBeing)) return false;
        HumanBeing that = (HumanBeing) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}