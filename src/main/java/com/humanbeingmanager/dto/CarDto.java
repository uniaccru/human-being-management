package com.humanbeingmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CarDto {
    
    private Long id;
    
    @NotBlank(message = "Car name cannot be blank")
    @Size(max = 50, message = "Car name must be 50 characters or less")
    private String name;
    
    private boolean cool;

    public CarDto() {}

    public CarDto(Long id, String name, boolean cool) {
        this.id = id;
        this.name = name;
        this.cool = cool;
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

    public boolean isCool() {
        return cool;
    }

    public void setCool(boolean cool) {
        this.cool = cool;
    }

    @Override
    public String toString() {
        return "CarDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", cool=" + cool +
                '}';
    }
}


