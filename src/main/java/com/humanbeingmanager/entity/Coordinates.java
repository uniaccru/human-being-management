package com.humanbeingmanager.entity;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

@Embeddable
public class Coordinates {
    
    @NotNull(message = "X coordinate cannot be null")
    private Integer x;
    
    @DecimalMin(value = "-1000.0", inclusive = false, message = "Y coordinate must be greater than -1000")
    private double y;

    public Coordinates() {}

    public Coordinates(Integer x, double y) {
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
        return "Coordinates{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinates)) return false;
        Coordinates that = (Coordinates) o;
        return Double.compare(that.y, y) == 0 &&
                x != null ? x.equals(that.x) : that.x == null;
    }

    @Override
    public int hashCode() {
        int result = x != null ? x.hashCode() : 0;
        long temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}