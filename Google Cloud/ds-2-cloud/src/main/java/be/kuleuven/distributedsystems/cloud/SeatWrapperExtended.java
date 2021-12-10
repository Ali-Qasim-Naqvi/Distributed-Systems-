package be.kuleuven.distributedsystems.cloud;

import java.util.UUID;

public class SeatWrapperExtended {
    private String seatId;
    private String time;
    private String type;
    private String name;
    private Double price;
    private Boolean availability;

    public SeatWrapperExtended(String seatId,String time, String type, String name, Double price) {
        this.seatId = seatId;
        this.time = time;
        this.type = type;
        this.name = name;
        this.price = price;
        this.availability = true;
    }
    public SeatWrapperExtended() {
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public String getSeatId() {return seatId;}

    public void setSeatId(String seatId) { this.seatId = seatId;}

    public void setPrice(Double price) {this.price = price;}

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() { return price; }

    public Boolean getAvailability() { return availability; }

    public void setAvailability(Boolean availability) { this.availability = availability; }
}

