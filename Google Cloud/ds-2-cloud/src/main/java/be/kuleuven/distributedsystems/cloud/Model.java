package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

@Component
public class Model {
    private List<Booking> bookings = new ArrayList<>();
    String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    String baseURL = "https://reliabletheatrecompany.com/";
    @Autowired WebClient.Builder webClientBuilder;

    public List<Show> getShows() {
        var shows = webClientBuilder
                                    .baseUrl(baseURL)
                                    .build()
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .pathSegment("shows")
                                            .queryParam("key",API_KEY)
                                            .build())
                                    .retrieve()
                                    .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                                    .block()
                                    .getContent();
        return new ArrayList<>(shows);
    }

    public Show getShow(String company, UUID showId) {
        var show = webClientBuilder
                .baseUrl("https://"+company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows",showId.toString())
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Show>() {})
                .block();
        return show;
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        var showTimes = webClientBuilder
                .baseUrl("https://"+company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows",showId.toString(),"times")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<LocalDateTime>>() {})
                .block()
                .getContent();
        return new ArrayList<>(showTimes);
    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        var seats = webClientBuilder
                .baseUrl("https://"+company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows",showId.toString(),"seats")
                        .queryParam("time",time)
                        .queryParam("available","true")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                .block()
                .getContent();
        return new ArrayList<>(seats);
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        var seat = webClientBuilder
                .baseUrl("https://"+company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows",showId.toString(),"seats", seatId.toString())
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Seat>() {})
                .block();
        return seat;
    }

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        var ticket = webClientBuilder
                .baseUrl("https://"+company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows",showId.toString(),"seats", seatId.toString(), "ticket")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                .block();
        return ticket;
    }

    public List<Booking> getBookings(String customer) {
        List<Booking> bookingList = bookings.stream().filter(booking -> booking.getCustomer().equals(customer)).collect(Collectors.toList());
        return bookingList;
    }

    public List<Booking> getAllBookings() {
        return bookings;
    }

    public Set<String> getBestCustomers() {
        Map<String, Integer> customerBookings = new HashMap<String, Integer>();
        for(var entry:bookings){
            customerBookings.put(entry.getCustomer(),0);
        }
        for(var entry:bookings){
            customerBookings.put(entry.getCustomer(),customerBookings.get(entry.getCustomer()) + entry.getTickets().size());
        }
        Integer largestVal = null;
        Set<String> customers = new HashSet<>();
        for (Map.Entry<String, Integer> i : customerBookings.entrySet()){
            if (largestVal == null || largestVal  < i.getValue()){
                largestVal = i.getValue();
                customers.clear();
                customers.add(i.getKey());
            }else if (largestVal == i.getValue()){
                customers.add(i.getKey());
            }
        }
//        Stream<Map.Entry<String, Integer>> customerBookings2 = customerBookings.entrySet().stream().filter(customer -> customer.getValue().equals(Collections.max(customerBookings.values())));
//        Set<String> customers = customerBookings2.keySet();
        return customers;
    }

    public void confirmQuotes(List<Quote> quotes, String customer) {
        List<Ticket> tickets = new ArrayList<>();
        for(var quote:quotes){
            tickets.add(webClientBuilder
                    .baseUrl("https://" + quote.getCompany())
                    .build()
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("shows",quote.getShowId().toString(),"seats", quote.getSeatId().toString(), "ticket")
                            .queryParam("customer",customer)
                            .queryParam("key",API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                    .block());
        }
        Booking tempBooking = new Booking(UUID.randomUUID(), LocalDateTime.now(),tickets,customer);
        bookings.add(tempBooking);
    }
}
