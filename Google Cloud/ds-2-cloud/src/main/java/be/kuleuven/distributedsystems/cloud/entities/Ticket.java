package be.kuleuven.distributedsystems.cloud.entities;

import java.io.Serializable;
import java.util.UUID;

public class Ticket implements Serializable {
    private String company;
    private UUID showId;
    private UUID seatId;
    private UUID ticketId;
    private String customer;

    public Ticket() {
    }

    public Ticket(String company, UUID showId, UUID seatId, UUID ticketId, String customer) {
        this.company = company;
        this.showId = showId;
        this.seatId = seatId;
        this.ticketId = ticketId;
        this.customer = customer;
    }

    public String getCompany() {
        return company;
    }

    public UUID getShowId() {
        return showId;
    }

    public UUID getSeatId() {
        return this.seatId;
    }

    public UUID getTicketId() {
        return this.ticketId;
    }

    public String getCustomer() {
        return this.customer;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setShowId(UUID showId) {
        this.showId = showId;
    }

    public void setSeatId(UUID seatId) {
        this.seatId = seatId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ticket)) {
            return false;
        }
        var other = (Ticket) o;
        return this.ticketId.equals(other.ticketId)
                && this.seatId.equals(other.seatId)
                && this.showId.equals(other.showId)
                && this.company.equals(other.company);
    }

    @Override
    public int hashCode() {
        return this.company.hashCode() * this.showId.hashCode() * this.seatId.hashCode() * this.ticketId.hashCode();
    }
}
