package com.scale.order.application.controller;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.scale.domain.DomainError;
import com.scale.domain.Order;
import com.scale.domain.Product;
import com.scale.domain.ShoppingCart;
import com.scale.order.*;
import com.scale.order.application.usecases.ConfirmOrder;
import com.scale.order.application.usecases.GenerateOrder;
import com.scale.order.application.usecases.UpdateOrder;
import com.scale.order.domain.repository.OrderRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class OrderManagementGRPCController extends OrderServiceGrpc.OrderServiceImplBase {
    @NonNull GenerateOrder generateOrder;
    @NonNull UpdateOrder updateOrder;
    @NonNull ConfirmOrder confirmOrder;
    @NonNull OrderRepository orderRepository;

    @Override
    public void createOrder(ShoppingCartDto request, StreamObserver<OrderDto> responseObserver) {
        var order = generateOrder.fromShoppingCart(getShoppingCartFromDto(request));

        log.info("Order {} was generated using gRPC", order.getId().value());

        responseObserver.onNext(convertOrderToDto(order));
        responseObserver.onCompleted();
    }

    @Override
    public void updateOrderAddress(AddressChange request, StreamObserver<Empty> responseObserver) {
        if (request.getId().isBlank()) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Order id is mandatory")
                    .asRuntimeException());
            return;
        }
        if (request.getAddress().isBlank()) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Address cannot be blank")
                    .asRuntimeException());
            return;
        }

        updateOrder.changeAddress(Order.OrderId.of(request.getId()), request.getAddress());

        log.info("Order address was updated using gRPC");

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void confirm(OrderIdAndReceipt request, StreamObserver<Empty> responseObserver) {
        if (request.getOrderId().isBlank()) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Order id is mandatory")
                    .asRuntimeException());
            return;
        }

        if (request.getPaymentReceipt().isBlank()) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Receipt number is mandatory")
                    .asRuntimeException());
            return;
        }

        try {
            confirmOrder.withPaymentReceipt(Order.OrderId.of(request.getOrderId()), request.getPaymentReceipt());
        } catch (DomainError e) {
            e.printStackTrace();
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unknown error while confirming order " + request.getOrderId())
                    .asRuntimeException());
            return;
        }

        log.info("Order {} was confirmed using gRPC", request.getOrderId());

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    // GRPC-related methods
    private ShoppingCart getShoppingCartFromDto(ShoppingCartDto cart) {
        var items = cart.getItemsList()
                .stream()
                .map(this::getShoppingCartItemFromDto)
                .collect(Collectors.toList());

        return ShoppingCart.builder()
                .clientId(ShoppingCart.ClientId.of(cart.getClientId()))
                .sessionId(cart.getSessionId())
                .createdAt(toUTCDateTime(cart.getCartDateTime()))
                .numberOfClientsOnSameSession(cart.getNumberOfCustomers())
                .address(cart.getAddress())
                .zipCode(cart.getZipCode())
                .city(cart.getCity())
                .state(cart.getState())
                .country(cart.getCountry())
                .isFirst(cart.getIsFirst())
                .isLast(cart.getIsLast())
                .items(items)
                .build();
    }

    private ShoppingCart.ShoppingCartItem getShoppingCartItemFromDto(OrderItemDto orderItem) {
        return ShoppingCart.ShoppingCartItem.builder()
                .id(orderItem.getId()) // TODO: Check
                .product(getProductFromDto(orderItem.getProduct()))
                .quantity(orderItem.getQuantity())
                .build();
    }

    private Product getProductFromDto(com.scale.order.ProductDto product) {
        return Product.builder()
                .id(product.getId())
                .name(product.getName())
                .price(BigDecimal.valueOf(product.getPrice()))
                .inStock(product.getStock())
                .build();
    }

    private OrderDto convertOrderToDto(Order order) {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        Timestamp orderDate = Timestamp.newBuilder()
                .setSeconds(now.toEpochSecond())
                .setNanos(now.getNano())
                .build();
        return OrderDto.newBuilder()
                .setId(order.getId().getValue())
                .setDate(orderDate)
                .setFullAddress(order.getFullAddress())
                .addAllItems(
                        order.getItems().stream()
                            .map(this::convertToOrderItemDto)
                            .collect(Collectors.toList())
                ).build();
    }

    private com.scale.order.OrderItemDto convertToOrderItemDto(Order.OrderItem item) {
        return com.scale.order.OrderItemDto.newBuilder()
                .setId(item.getId())
                .setProduct(convertToProductDto(item.getProduct()))
                .setQuantity(item.getQuantity())
                .build();
    }

    private com.scale.order.ProductDto convertToProductDto(Product product) {
        return com.scale.order.ProductDto.newBuilder()
                .setId(product.getId())
                .setName(product.getName())
                .setPrice(product.getPrice().floatValue())
                .setStock(product.getInStock())
                .build();
    }

    private ZonedDateTime toUTCDateTime(Timestamp input) {
        return Instant
                .ofEpochSecond( input.getSeconds(), input.getNanos() )
                .atOffset( ZoneOffset.UTC )
                .toZonedDateTime();
    }

}
