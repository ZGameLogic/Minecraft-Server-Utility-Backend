package com.zgamelogic.controllers;

import com.zgamelogic.data.database.user.User;
import com.zgamelogic.data.database.user.UserRepository;
import com.zgamelogic.data.services.setup.SetupItem;
import com.zgamelogic.data.services.setup.SetupStatus;
import com.zgamelogic.data.services.setup.SetupStatusWithError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.io.File;

@RestController
@RequestMapping("setup")
@Slf4j
public class SetupController {

    private final UserRepository userRepository;

    @Autowired
    public SetupController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("status")
    private SetupStatus status(){
        return new SetupStatus(userRepository.count() > 0);
    }

    @PostMapping
    private SetupStatus setValue(@RequestBody SetupItem item){
        boolean setup = userRepository.count() > 0;
        if(setup){
            return new SetupStatusWithError(
                    setup,
                    "Initial user already setup. This endpoint is now useless"
            );
        }
        userRepository.save(new User(item.username(), item.password()));
        return new SetupStatus(userRepository.count() > 0);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    private SetupStatusWithError exceptionHandler(RuntimeException ex, WebRequest request){
        return new SetupStatusWithError(
                userRepository.count() > 0,
                "Unable to process JSON body. Needs keys: username, password"
        );
    }
}
