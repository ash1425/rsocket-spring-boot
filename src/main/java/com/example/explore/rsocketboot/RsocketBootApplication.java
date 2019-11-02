package com.example.explore.rsocketboot;

import io.micrometer.core.instrument.MeterRegistry;
import io.rsocket.micrometer.MicrometerDuplexConnectionInterceptor;
import io.rsocket.micrometer.MicrometerRSocketInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;

@SpringBootApplication
public class RsocketBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(RsocketBootApplication.class, args);
    }

}

@Controller
class RSocketController {

    private Random random = new Random();

    @MessageMapping("/try")
    public Mono<String> echo(String request) {
        return Mono.delay(Duration.ofMillis(5 * random.nextInt(20)))
                .map(aLong -> "Echo : " + request);
    }
}

@RestController
class HTTPController {

    private final RSocketRequester.Builder builder;

    HTTPController(RSocketRequester.Builder builder) {
        this.builder = builder;
    }

    @GetMapping("/toRSocket")
    public Flux<String> send(@RequestParam("name") String name) {
        return builder.connectTcp("localhost", 7777)
                .flatMapMany(rSocketRequester -> Flux.range(0, 10000)
                        .flatMap(integer -> rSocketRequester.route("/try")
                                .data(name)
                                .retrieveMono(String.class)));
    }
}

@Configuration
class RSocketConfig {

    @Bean
    public ServerRSocketFactoryProcessor serverRSocketFactoryProcessor(MeterRegistry meterRegistry) {
        return factory -> factory.addResponderPlugin(new MicrometerRSocketInterceptor(meterRegistry))
                .addConnectionPlugin(new MicrometerDuplexConnectionInterceptor(meterRegistry));
    }
}

