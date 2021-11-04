package org.example.AlmaOnline.required;


import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.example.AlmaOnline.defaults.Initializer;
import org.example.AlmaOnline.provided.client.AlmaOnlineClientAdapter;
import org.example.AlmaOnline.provided.server.AlmaOnlineServerAdapter;
import org.example.AlmaOnline.provided.service.*;
import org.example.AlmaOnline.server.*;
import org.example.AlmaOnline.provided.service.exceptions.OrderException;

import java.util.*;
import java.util.stream.Collectors;

// AlmaOnlineServerGrpcAdapter implements the grpc-server side of the application.
// The implementation should not contain any additional business logic, only implement
// the code here that is required to couple your IDL definitions to the provided business logic.
public class AlmaOnlineServerGrpcAdapter extends AlmaOnlineGrpc.AlmaOnlineImplBase implements AlmaOnlineServerAdapter {

    // the service field contains the AlmaOnline service that the server will
    // call during testing.
    private final AlmaOnlineService service;

    public AlmaOnlineServerGrpcAdapter() {
        this.service = this.getInitializer().initialize();
    }

    // -- Put the code for your implementation down below -- //
    @Override
    public void getRestaurants(EmptyAck request, StreamObserver<ListOfRestaurantInfoResponse> responseObserver) {
        List <Restaurant> restaurants = new ArrayList<>(service.getRestaurants());
        ListOfRestaurantInfoResponse.Builder listOfRestaurantInfoResponse = ListOfRestaurantInfoResponse.newBuilder();
        for (Restaurant entry:restaurants){
            RestaurantInfo.Builder restaurantInfo = RestaurantInfo.newBuilder();
            restaurantInfo.setId(entry.getId()).setName(entry.getName());
            restaurantInfo.build();
            listOfRestaurantInfoResponse.addListOfRestaurantInfo(restaurantInfo);
        }
        ListOfRestaurantInfoResponse listOfRestaurantInfoResponse1 = listOfRestaurantInfoResponse.build();
        responseObserver.onNext(listOfRestaurantInfoResponse1);
        responseObserver.onCompleted();
    }

    @Override
    public void getMenu(RestaurantIdRequest request, StreamObserver<MenuInfoResponse> responseObserver) {
        Map<String, Double> menuItems = new HashMap<String, Double>();
        for (var entry :  service.getRestaurantMenu(request.getId()).get().getItems()) {
            menuItems.put(entry.getName(),entry.getPrice());
        }
        MenuInfoResponse menuInfoResponse = MenuInfoResponse.newBuilder().putAllItems(menuItems).build();
        responseObserver.onNext(menuInfoResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getOrder(GetOrderRequest request, StreamObserver<BaseOrderInfoResponse> responseObserver) throws InvalidProtocolBufferException {
        Order customerOrder = service.getOrder(request.getRestaurantId(),request.getOrderId()).get();
        long createDate = customerOrder.getCreationDate().getTime();
        List<Item> items = new ArrayList<Item>(customerOrder.getItems());
        List<ItemInfo> itemInfos = new ArrayList<ItemInfo>();
        for (var entry : items){
            ItemInfo itemInfoTemp = ItemInfo.newBuilder().setName(entry.getName()).setPrice(entry.getPrice()).build();
            itemInfos.add(itemInfoTemp);
        }
        BaseOrderInfoResponse baseOrderInfoResponse = BaseOrderInfoResponse.newBuilder().setCustomer(customerOrder.getCustomer()).setCreateDate(createDate).addAllItems(itemInfos).build();
        responseObserver.onNext(baseOrderInfoResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void createDineInOrder(DineInOrderQuoteRequest request, StreamObserver<EmptyAck> responseObserver) throws OrderException {
        Date reservationDate = new java.util.Date(request.getReservationDate());
        Date currentDate = new java.util.Date();
        DineInOrderQuote dineInOrderQuote = new DineInOrderQuote(request.getOrderId(),currentDate,request.getCustomer(),request.getItemsList(),reservationDate);
        service.createDineInOrder(request.getRestaurantId(),dineInOrderQuote);
        EmptyAck emptyAck = EmptyAck.newBuilder().build();
        responseObserver.onNext(emptyAck);
        responseObserver.onCompleted();
    }

    @Override
    public void createDeliveryOrder(DeliveryOrderRequest request, StreamObserver<EmptyAck> responseObserver) throws OrderException {
        Date currentDate = new java.util.Date();
        DeliveryOrderQuote deliveryOrderQuote = new DeliveryOrderQuote(request.getOrderId(),currentDate, request.getCustomer(), request.getItemsList(), request.getDeliveryAddress());
        service.createDeliveryOrder(request.getRestaurantId(),deliveryOrderQuote);
        EmptyAck emptyAck = EmptyAck.newBuilder().build();
        responseObserver.onNext(emptyAck);
        responseObserver.onCompleted();
    }
}
