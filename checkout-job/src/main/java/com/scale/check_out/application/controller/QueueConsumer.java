package com.scale.check_out.application.controller;

import reactor.core.publisher.Flux;

public interface QueueConsumer {
    Flux<Void> start();
    void stop();
}
