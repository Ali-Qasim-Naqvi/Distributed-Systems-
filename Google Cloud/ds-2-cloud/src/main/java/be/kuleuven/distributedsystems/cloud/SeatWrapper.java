package be.kuleuven.distributedsystems.cloud;

import java.util.List;

public class SeatWrapper {
    private String time;
    private String type;
    private String name;
    private String price;

    public SeatWrapper(String time, String type, String name, String price) {
        this.time = time;
        this.type = type;
        this.name = name;
        this.price = price;
    }
    public SeatWrapper() {
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

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
