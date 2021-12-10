package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Ticket;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class BookingWrapper implements Serializable{
    private UUID id;
    private LocalDateTime time;
    private List<Ticket> tickets;
    private String customer;

    public BookingWrapper(UUID id, LocalDateTime time, List<Ticket> tickets, String customer) {
        this.id = id;
        this.time = time;
        this.tickets = tickets;
        this.customer = customer;
    }

    public BookingWrapper(){

    }
    public UUID getId() {
        return this.id;
    }

    public LocalDateTime getTime() {
        return this.time;
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }

    public String getCustomer() {
        return this.customer;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String toString(BookingWrapper bookingWrapper) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = null;
        outputStream = new ObjectOutputStream( baos );
        outputStream.writeObject( bookingWrapper );
        outputStream.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public BookingWrapper fromString( String message ) throws IOException , ClassNotFoundException {
        byte [] data = Base64.getDecoder().decode( message);
        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( data ) );
        Object o  = ois.readObject();
        ois.close();
        return (BookingWrapper) o;
    }
}
