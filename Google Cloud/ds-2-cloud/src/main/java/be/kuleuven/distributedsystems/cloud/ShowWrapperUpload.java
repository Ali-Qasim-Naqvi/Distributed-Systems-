package be.kuleuven.distributedsystems.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShowWrapperUpload {


    private String name;
    private String location;
    private String image;
    private List<SeatWrapperExtended> seats;

    public ShowWrapperUpload(){
    }

    public ShowWrapperUpload(String name, String location, String image, List<SeatWrapper> seats) {
        this.name = name;
        this.location = location;
        this.image = image;
        List<SeatWrapperExtended> tempSeats = new ArrayList<>();
        for (var entry : seats){
            tempSeats.add(new SeatWrapperExtended(UUID.randomUUID().toString(), entry.getTime(), entry.getType(), entry.getName(), Double.valueOf(entry.getPrice())));
        }
        this.seats = tempSeats;
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

    public List<SeatWrapperExtended> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatWrapperExtended> seats) {
        this.seats = seats;
    }
}

