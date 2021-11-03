package org.example.AlmaOnline.required;

import com.google.common.util.concurrent.ListenableFuture;
import org.example.AlmaOnline.provided.client.*;
import org.example.AlmaOnline.provided.client.ItemInfo;
import org.example.AlmaOnline.provided.client.RestaurantInfo;
import org.example.AlmaOnline.server.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// AlmaOnlineClientGrpcAdapter provides your own implementation of the AlmaOnlineClientAdapter
public class AlmaOnlineClientGrpcAdapter implements AlmaOnlineClientAdapter {
    // getRestaurants should retrieve the information on all the available restaurants.
    @Override
    public List<RestaurantInfo> getRestaurants(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub) {
        EmptyAck emptyAck = EmptyAck.newBuilder().build();
        ListOfRestaurantInfoResponse listOfRestaurantInfoResponse = stub.getRestaurants(emptyAck);
        List<RestaurantInfo> restaurantInfo = new ArrayList<RestaurantInfo>();
        for (org.example.AlmaOnline.server.RestaurantInfo entry : listOfRestaurantInfoResponse.getListOfRestaurantInfoList()) {
            RestaurantInfo restaurantInfoTemp = new RestaurantInfo(entry.getId(), entry.getName());
            restaurantInfo.add(restaurantInfoTemp);
        }
        return restaurantInfo;
    }

    // getMenu should return the menu of a given restaurant
    @Override
    public MenuInfo getMenu(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub, String restaurantId) {
        RestaurantIdRequest restaurantIdRequest = RestaurantIdRequest.newBuilder().setId(restaurantId).build();
        MenuInfoResponse menuInfoResponse = stub.getMenu(restaurantIdRequest);
        MenuInfo menuInfo = new MenuInfo(menuInfoResponse.getItemsMap());
        return menuInfo;
    }

    // createDineInOrder should create the given dine-in order at the AlmaOnline server
    @Override
    public ListenableFuture<?> createDineInOrder(AlmaOnlineGrpc.AlmaOnlineFutureStub stub, DineInOrderQuote order) {
        long reservationDate = order.getReservationDate().getTime();
        DineInOrderQuoteRequest dineInOrderQuote = DineInOrderQuoteRequest.newBuilder().setRestaurantId(order.getRestaurantId()).setOrderId(order.getOrderId()).setCustomer(order.getCustomer()).addAllItems(order.getItems()).setReservationDate(reservationDate).build();
        ListenableFuture<EmptyAck> emptyAck = stub.createDineInOrder(dineInOrderQuote);
        return emptyAck;
    }

    // createDeliveryOrder should create the given delivery order at the AlmaOnline server
    @Override
    public ListenableFuture<?> createDeliveryOrder(AlmaOnlineGrpc.AlmaOnlineFutureStub stub, DeliveryOrder order) {
        DeliveryOrderRequest deliveryOrder = DeliveryOrderRequest.newBuilder().setRestaurantId(order.getRestaurantId()).setOrderId(order.getOrderId()).setCustomer(order.getCustomer()).addAllItems(order.getItems()).setDeliveryAddress(order.getDeliveryAddress()).build();
        ListenableFuture<EmptyAck> emptyAck = stub.createDeliveryOrder(deliveryOrder);
        return emptyAck;
    }

    // getOrder should retrieve the order information at the AlmaOnline server given the restaurant the order is
    // placed at and the id of the order.
    @Override
    public BaseOrderInfo getOrder(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub, String restaurantId, String orderId) {
        GetOrderRequest getOrderRequest = GetOrderRequest.newBuilder().setRestaurantId(restaurantId).setOrderId(orderId).build();
        BaseOrderInfoResponse baseOrderInfoResponse = stub.getOrder(getOrderRequest);
        Date createDate = new java.util.Date(baseOrderInfoResponse.getCreateDate());
        List<ItemInfo> itemInfo = new ArrayList<ItemInfo>();
        for (org.example.AlmaOnline.server.ItemInfo entry : baseOrderInfoResponse.getItemsList()) {
            RestaurantInfo restaurantInfoTemp = new RestaurantInfo(entry.getId(), entry.getName());
            restaurantInfo.add(restaurantInfoTemp);
        }
        BaseOrderInfo baseOrderInfo = new BaseOrderInfo(baseOrderInfoResponse.getCustomer(),createDate,);
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
