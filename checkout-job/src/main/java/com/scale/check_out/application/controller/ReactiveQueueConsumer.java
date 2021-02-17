package com.scale.check_out.application.controller;

import reactor.core.publisher.Flux;

public interface ReactiveQueueConsumer {
    Flux<Void> start();
    void stop();
}
