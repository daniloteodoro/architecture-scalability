package com.scale.payment.application.controller;

import com.scale.domain.Order;
import com.scale.payment.OrderPaymentDetailMessage;
import com.scale.payment.PaymentRequestMessage;
import com.scale.payment.PaymentServiceGrpc;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class PaymentGRPCController extends PaymentServiceGrpc.PaymentServiceImplBase {
    @NonNull PayOrder payOrder;
    @NonNull PaymentRepository paymentRepository;

    @Override
    public void pay(PaymentRequestMessage request, StreamObserver<OrderPaymentDetailMessage> responseObserver) {
        if (request.getOrderId().isBlank()) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Order id is mandatory")
                    .asRuntimeException());
            return;
        }
        if (request.getTotal() <= 0d) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Total price must be greater than zero")
                    .asRuntimeException());
            return;
        }

        // Pay order with id XXX, total 342.09 with card number NNNN
        Card sampleCard = new Card("1234", (short)333, new Card.ExpirationDate("10/2030"), Money.of(3000.0));
        payOrder.using(Order.OrderId.of(request.getOrderId()), Money.of(request.getTotal()), sampleCard);

        log.info("Order {} was paid using gRPC", request.getOrderId());

//        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
//
//    @Override
//    public void createOrder(ShoppingCartDto request, StreamObserver<OrderDto> responseObserver) {
//        var order = generateOrder.fromShoppingCart(getShoppingCartFromDto(request));
//
//        log.info("Order {} was generated using gRPC", order.getId().value());
//
//        responseObserver.onNext(convertOrderToDto(order));
//        responseObserver.onCompleted();
//    }
//
//    @Override
//    public void updateOrderAddress(AddressChange request, StreamObserver<Empty> responseObserver) {
//        if (request.getId().isBlank()) {
//            responseObserver.onError(Status.FAILED_PRECONDITION
//                    .withDescription("Order id is mandatory")
//                    .asRuntimeException());
//            return;
//        }
//        if (request.getAddress().isBlank()) {
//            responseObserver.onError(Status.FAILED_PRECONDITION
//                    .withDescription("Address cannot be blank")
//                    .asRuntimeException());
//            return;
//        }
//
//        updateOrder.changeAddress(Order.OrderId.of(request.getId()), request.getAddress());
//
//        log.info("Order address was updated using gRPC");
//
//        responseObserver.onNext(Empty.newBuilder().build());
//        responseObserver.onCompleted();
//    }
//
//    @Override
//    public void confirm(OrderId request, StreamObserver<Empty> responseObserver) {
//        if (request.getId().isBlank()) {
//            responseObserver.onError(Status.FAILED_PRECONDITION
//                    .withDescription("Order id is mandatory")
//                    .asRuntimeException());
//            return;
//        }
//
//        confirmOrder.withId(Order.OrderId.of(request.getId()));
//
//        log.info("Order {} was confirmed using gRPC", request.getId());
//
//        responseObserver.onNext(Empty.newBuilder().build());
//        responseObserver.onCompleted();
//    }
//
//    // GRPC-related methods
//    private ShoppingCart getShoppingCartFromDto(ShoppingCartDto cart) {
//        var items = cart.getItemsList()
//                .stream()
//                .map(this::getShoppingCartItemFromDto)
//                .collect(Collectors.toList());
//
//        return ShoppingCart.forClient(ShoppingCart.ClientId.of(cart.getClientId()), cart.getSessionId(), toUTCDateTime(cart.getCartDateTime()),
//                cart.getNumberOfCustomers(), cart.getIsFirst(), cart.getIsLast(), items);
//    }
//
//    private ShoppingCart.ShoppingCartItem getShoppingCartItemFromDto(OrderItemDto orderItem) {
//        return ShoppingCart.ShoppingCartItem.builder()
//                .id(orderItem.getId()) // TODO: Check
//                .product(getProductFromDto(orderItem.getProduct()))
//                .quantity(orderItem.getQuantity())
//                .build();
//    }
//
//    private Product getProductFromDto(com.scale.order.ProductDto product) {
//        return Product.builder()
//                .id(product.getId())
//                .name(product.getName())
//                .price(BigDecimal.valueOf(product.getPrice()))
//                .inStock(product.getStock())
//                .build();
//    }
//
//    private OrderDto convertOrderToDto(Order order) {
//        var now = ZonedDateTime.now(ZoneOffset.UTC);
//        Timestamp orderDate = Timestamp.newBuilder()
//                .setSeconds(now.toEpochSecond())
//                .setNanos(now.getNano())
//                .build();
//        return OrderDto.newBuilder()
//                .setId(order.getId().getValue())
//                .setDate(orderDate)
//                .addAllItems(
//                        order.getItems().stream()
//                            .map(this::convertToOrderItemDto)
//                            .collect(Collectors.toList())
//                ).build();
//    }
//
//    private com.scale.order.OrderItemDto convertToOrderItemDto(Order.OrderItem item) {
//        return com.scale.order.OrderItemDto.newBuilder()
//                .setId(item.getId())
//                .setProduct(convertToProductDto(item.getProduct()))
//                .setQuantity(item.getQuantity())
//                .build();
//    }
//
//    private com.scale.order.ProductDto convertToProductDto(Product product) {
//        return com.scale.order.ProductDto.newBuilder()
//                .setId(product.getId())
//                .setName(product.getName())
//                .setPrice(product.getPrice().floatValue())
//                .setStock(product.getInStock())
//                .build();
//    }
//
//    private ZonedDateTime toUTCDateTime(Timestamp input) {
//        return Instant
//                .ofEpochSecond( input.getSeconds(), input.getNanos() )
//                .atOffset( ZoneOffset.UTC )
//                .toZonedDateTime();
//    }

}
