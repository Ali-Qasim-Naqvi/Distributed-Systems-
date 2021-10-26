package org.example.AlmaOnline.required;

import com.google.common.util.concurrent.ListenableFuture;
import org.example.AlmaOnline.provided.client.*;
import org.example.AlmaOnline.server.*;

import java.util.List;

// AlmaOnlineClientGrpcAdapter provides your own implementation of the AlmaOnlineClientAdapter
public class AlmaOnlineClientGrpcAdapter implements AlmaOnlineClientAdapter {
    // getRestaurants should retrieve the information on all the available restaurants.
    @Override
    public List<RestaurantInfo> getRestaurants(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub) {
        EmptyAck emptyAck = EmptyAck.newBuilder().build();
        ListOfRestaurantInfoResponse listOfRestaurantInfoResponse = stub.getRestaurants(emptyAck);
        List<RestaurantInfo> restaurantInfo = new ArrayList<RestaurantInfo>(listOfRestaurantInfoResponse);
        return restaurantInfo;
    }

    // getMenu should return the menu of a given restaurant
    @Override
    public MenuInfo getMenu(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub, String restaurantId) {
        RestaurantIdRequest restaurantId = RestaurantIdRequest.newBuilder().setid(restaurantId).build();
        MenuInfoResponse menuInfoResponse = stub.getMenu(restaurantId);
        MenuInfo menuInfo = new menuInfo(menuInfoResponse.getItems);
        return menuInfo;
    }

    // createDineInOrder should create the given dine-in order at the AlmaOnline server
    @Override
    public ListenableFuture<?> createDineInOrder(AlmaOnlineGrpc.AlmaOnlineFutureStub stub, DineInOrderQuote order) {
        long reservationDate = order.getReservationDate().getTime();
        DineInOrderQuoteRequest dineInOrderQuote = DineInOrderQuoteRequest.newBuilder().setrestaurantId(order.getRestaurantId()).setorderId(order.getOrderId()).setcustomer(order.getCustomer()).setitems(order.getItems()).setreservationDate(reservationDate).build();
        EmptyAck emptyAck = stub.createDineInOrder(dineInOrderQuote);
        return emptyAck;
    }

    // createDeliveryOrder should create the given delivery order at the AlmaOnline server
    @Override
    public ListenableFuture<?> createDeliveryOrder(AlmaOnlineGrpc.AlmaOnlineFutureStub stub, DeliveryOrder order) {
        DeliveryOrderRequest deliveryOrder = DeliveryOrderRequest.newBuilder().setrestaurantId(order.getRestaurantId()).setorderId(order.getOrderId()).setcustomer(order.getCustomer()).setitems(order.getItems()).setdeliveryAddress(order.getDeliveryAddress()).build();
        EmptyAck emptyAck = stub.createDeliveryOrder(deliveryOrder);
        return emptyAck;
    }

    // getOrder should retrieve the order information at the AlmaOnline server given the restaurant the order is
    // placed at and the id of the order.
    @Override
    public BaseOrderInfo getOrder(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub, String restaurantId, String orderId) {
        GetOrderRequest getOrderRequest = GetOrderRequest.newBuilder().setrestaurantId(restaurantId).setorderId(orderId).build();
        BaseOrderInfoResponse baseOrderInfoResponse = stub.getOrder(getOrderRequest);
        Date createDate = new java.util.Date(baseOrderInfoResponse.getcreateDate());
        BaseOrderInfo baseOrderInfo = new BaseOrderInfo(baseOrderInfoResponse.getcustomer(),createDate,baseOrderInfoResponse.getitems());
        return baseOrderInfo;
    }

    // getScript returns the script the application will run during testing.
    // You can leave the default implementation, as it will test most of the functionality.
    // Alternatively, you can provide your own implementation to test your own edge-cases.
    @Override
    public AppScript getScript() {
        return AlmaOnlineClientAdapter.super.getScript();
    }
}
