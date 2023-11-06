package com.zgamelogic.controllers;

import com.zgamelogic.data.services.Greeting;
import com.zgamelogic.data.services.HelloMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class MinecraftController {

    @MessageMapping("/hello")
    @SendTo("/server/message")
    public Greeting greeting(HelloMessage message) {
        log.info("Got a client message");
        return new Greeting("Hello, " + message.getName() + "!");
    }
}
