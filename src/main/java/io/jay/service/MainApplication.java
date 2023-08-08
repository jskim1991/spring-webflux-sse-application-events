package io.jay.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import java.util.UUID;

@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}

@Configuration
class FluxConfiguration {
    @Bean
    public Sinks.Many<User> sink() {
        return Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
    }
}

@Component
class AppRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {

    }
}

record UserEvent(String type, User user) {}

@Controller
@ResponseBody
class CustomController {

    private final ApplicationEventPublisher publisher;
    private final Sinks.Many<User> sink;

    public CustomController(ApplicationEventPublisher publisher, Sinks.Many<User> sink) {
        this.publisher = publisher;
        this.sink = sink;
    }

    @GetMapping(value = "/publish")
    public Flux<User> publish() {
        return Flux.just("A", "B", "C").flatMap(
                name -> {
                    User user = new User(name);
                    publisher.publishEvent(new UserEvent("sign up", user));
                    return Flux.just(user);
                }
        );
    }


    @GetMapping(value = "/all", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<User> getUserStream() {
        return sink.asFlux();
    }

    @EventListener
    public void listen(UserEvent event) {
        sink.tryEmitNext(event.user());
    }
}

class User {

    private String id;
    private String name;

    public User() {
    }

    public User(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}