package be.kuleuven.distributedsystems.cloud;

import java.util.List;

public class ShowWrapper {
    private String name;
    private String location;
    private String image;
    private List<SeatWrapper> seats;

    public ShowWrapper(){
    }

    public ShowWrapper(String name, String location, String image, List<SeatWrapper> seats) {
        this.name = name;
        this.location = location;
        this.image = image;
        this.seats = seats;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<SeatWrapper> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatWrapper> seats) {
        this.seats = seats;
    }
}
