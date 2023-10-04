package com.zgamelogic.controllers;

import com.zgamelogic.data.database.configuration.ConfigurationItemRepository;
import com.zgamelogic.data.database.user.UserRepository;
import com.zgamelogic.data.setup.SetupItem;
import com.zgamelogic.data.setup.SetupStatus;
import com.zgamelogic.data.setup.SetupStatusWithError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping("setup")
@Slf4j
public class SetupController {

    private final ConfigurationItemRepository configurationItemRepository;
    private final UserRepository userRepository;

    @Autowired
    public SetupController(ConfigurationItemRepository configurationItemRepository, UserRepository userRepository) {
        this.configurationItemRepository = configurationItemRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("status")
    private SetupStatus status(){
        return new SetupStatus(
            configurationItemRepository.existsById("website port"),
            configurationItemRepository.existsById("initial user"),
            configurationItemRepository.existsById("initial pass")
        );
    }

    @PostMapping("value")
    private SetupStatus setValue(@RequestBody SetupItem item){
        System.out.println(item);
        return new SetupStatus(
            configurationItemRepository.existsById("website port"),
            configurationItemRepository.existsById("initial user"),
            configurationItemRepository.existsById("initial pass")
        );
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    private SetupStatusWithError exceptionHandler(RuntimeException ex, WebRequest request){
        String message = "Unable to process JSON body. Needs keys: name, value";
        return new SetupStatusWithError(
                configurationItemRepository.existsById("website port"),
                configurationItemRepository.existsById("initial user"),
                configurationItemRepository.existsById("initial pass"),
                message
        );
    }
}
