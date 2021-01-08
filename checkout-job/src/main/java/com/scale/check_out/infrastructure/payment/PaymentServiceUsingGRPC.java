package com.scale.check_out.infrastructure.payment;

import com.scale.check_out.application.services.payment.PayOrder;
import com.scale.check_out.application.services.payment.PaymentDto;
import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class PaymentServiceUsingGRPC implements PayOrder {
//    @NonNull OrderServiceGrpc.OrderServiceBlockingStub orderService;
//
//    @Override
//    public Order intoOrder(ShoppingCart shoppingCart) {
//        var result = orderService.createOrder(toShoppingCartDto(shoppingCart));
//        return toOrder(result);
//    }
//
//    @Override
//    public void changeAddress(Order order) {
//        orderService.updateOrderAddress(AddressChange.newBuilder()
//            .setId(order.getId().value())
//            .setAddress("Address updated from gRPC service")
//            .build());
//    }
//
//    @Override
//    public void withPaymentReceipt(Order order, PaymentDto.PaymentReceiptDto receipt) {
//        orderService.confirm(OrderId.newBuilder()
//            .setId(order.getId().value())
//            .build());
//    }
//
//    private ShoppingCartDto toShoppingCartDto(ShoppingCart shoppingCart) {
//        return ShoppingCartDto.newBuilder()
//                .setSessionId(shoppingCart.getSessionId())
//                .setCartDateTime(toProtoDateTime(shoppingCart.getCreatedAt()))
//                .setClientId(shoppingCart.getClientId().value())
//                .setAddress(shoppingCart.getAddress())
//                .setZipCode(shoppingCart.getZipCode())
//                .setCity(shoppingCart.getCity())
//                .setState(shoppingCart.getState())
//                .setCountry(shoppingCart.getCountry())
//                .addAllItems(shoppingCart.getItems().stream()
//                        .map(this::toItemDto)
//                        .collect(Collectors.toList()))
//                .setNumberOfCustomers(shoppingCart.getNumberOfClientsOnSameSession())
//                .setIsFirst(shoppingCart.getIsFirst())
//                .setIsLast(shoppingCart.getIsLast())
//                .build();
//    }
//
//    private OrderDto toOrderDto(Order input) {
//        return OrderDto.newBuilder()
//                .setId(input.getId().value())
//                .setDate(toProtoDateTime(input.getCreatedAt()))
//                .addAllItems(input.getItems().stream()
//                        .map(this::toItemDto)
//                        .collect(Collectors.toList()))
//                .build();
//    }
//
//    private OrderItemDto toItemDto(ShoppingCart.ShoppingCartItem item) {
//        return OrderItemDto.newBuilder()
//                .setId(item.getId())
//                .setQuantity(item.getQuantity())
//                .setProduct(toProductDto(item.getProduct()))
//                .build();
//    }
//
//    private OrderItemDto toItemDto(Order.OrderItem item) {
//        return OrderItemDto.newBuilder()
//                .setId(item.getId())
//                .setQuantity(item.getQuantity())
//                .setProduct(toProductDto(item.getProduct()))
//                .build();
//    }
//
//    private ProductDto toProductDto(Product input) {
//        return ProductDto.newBuilder()
//                .setId(input.getId())
//                .setName(input.getName())
//                .setStock(input.getInStock())
//                .setPrice(input.getPrice().floatValue())
//                .build();
//    }
//
//    private Order toOrder(OrderDto input) {
//        return Order.builder()
//                .id(Order.OrderId.of(input.getId()))
//                .createdAt(toUTCDateTime(input.getDate()))
//                .items(input.getItemsList().stream()
//                        .map(this::toOrderItem)
//                        .collect(Collectors.toList()))
//                .build();
//    }
//
//    private Order.OrderItem toOrderItem(OrderItemDto input) {
//        return Order.OrderItem.builder()
//                .id(input.getId())
//                .quantity(input.getQuantity())
//                .product(toProduct(input.getProduct()))
//                .build();
//    }
//
//    private Product toProduct(ProductDto input) {
//        return Product.builder()
//                .id(input.getId())
//                .name(input.getName())
//                .inStock(input.getStock())
//                .price(BigDecimal.valueOf(input.getPrice()))
//                .build();
//    }
//
//    private Timestamp toProtoDateTime(ZonedDateTime input) {
//        return Timestamp.newBuilder()
//                .setSeconds(input.toEpochSecond())
//                .setNanos(input.getNano())
//                .build();
//    }
//
//    private ZonedDateTime toUTCDateTime(Timestamp input) {
//        return Instant
//                .ofEpochSecond( input.getSeconds(), input.getNanos() )
//                .atOffset( ZoneOffset.UTC )
//                .toZonedDateTime();
//    }


    @Override
    public PaymentDto.PaymentReceiptDto with(ShoppingCart.ClientId clientId, Order order) {
        return null;
    }
}
