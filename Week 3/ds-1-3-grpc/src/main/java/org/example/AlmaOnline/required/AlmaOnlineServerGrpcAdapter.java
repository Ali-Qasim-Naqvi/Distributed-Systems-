package org.example.AlmaOnline.required;


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
        List <Restaurant> restaurants = new List<Restaurant>(service.getRestaurants());
        RestaurantInfo restaurantInfo = RestaurantInfo.newBuilder();
        ListOfRestaurantInfoResponse listOfRestaurantInfoResponse = ListOfRestaurantInfoResponse.newBuilder();
        for (Restaurant restaurant:restaurants){
            restaurantInfo.setid(restaurant.getId()).setname(restaurant.getName());
            restaurantInfo.build();
            listOfRestaurantInfoResponse.addlistOfRestaurantInfo(restaurantInfo);
        }
        listOfRestaurantInfoResponse.build();
        responseObserver.onNext(listOfRestaurantInfoResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getMenu(RestaurantIdRequest request, StreamObserver<MenuInfoResponse> responseObserver) {
        map<String,double> menuItems = new map<String,double> ();
        for (var entry : service.getRestaurantMenu(request.getid())) {
            menuItems.put(entry.getValue().getName(),entry.getValue().getPrice());
        }
        MenuInfoResponse menuInfoResponse = MenuInfoResponse.newBuilder().setitems(menuItems).build();
        responseObserver.onNext(menuInfoResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getOrder(GetOrderRequest request, StreamObserver<BaseOrderInfoResponse> responseObserver) {
        Order customerOrder = service.getOrder(request.getrestaurantId(),request.getorderId());
        long createDate = customerOrder.getCreationDate().getTime();
        BaseOrderInfoResponse baseOrderInfoResponse = BaseOrderInfoResponse.newBuilder().setcustomer(customerOrder.getCustomer()).setcreateDate(createDate).setitems(customerOrder.getItems()).build();
        responseObserver.onNext(baseOrderInfoResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void createDineInOrder(DineInOrderQuoteRequest request, StreamObserver<EmptyAck> responseObserver) {
        Date reservationDate = new java.util.Date(request.getreservationDate());

        //DineInOrderQuote dineInOrderQuote = new DineInOrderQuote()
        responseObserver.onNext(service.createDineInOrder(request.getrestaurantId(),request.getorderId()));
        responseObserver.onCompleted();
    }
}
