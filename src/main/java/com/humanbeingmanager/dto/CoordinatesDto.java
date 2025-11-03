package com.humanbeingmanager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

public class CoordinatesDto {
    
    @NotNull(message = "X coordinate cannot be null")
    private Integer x;
    
    @DecimalMin(value = "-1000.0", inclusive = false, message = "Y coordinate must be greater than -1000")
    private double y;

    public CoordinatesDto() {}

    public CoordinatesDto(Integer x, double y) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "CoordinatesDto{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}


