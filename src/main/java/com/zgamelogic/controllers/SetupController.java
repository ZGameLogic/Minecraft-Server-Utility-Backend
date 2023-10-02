package com.zgamelogic.controllers;

import com.zgamelogic.data.database.configuration.ConfigurationItemRepository;
import com.zgamelogic.data.database.user.User;
import com.zgamelogic.data.database.user.UserRepository;
import com.zgamelogic.data.database.user.UserSession;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class SetupController {

    private final ConfigurationItemRepository configurationItemRepository;
    private final UserRepository userRepository;

    @Autowired
    public SetupController(ConfigurationItemRepository configurationItemRepository, UserRepository userRepository) {
        this.configurationItemRepository = configurationItemRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    private void thing(){
//        User ben = new User();
//        ben.setUsername("k");
//        ben.setPassword("k");
//        userRepository.save(ben);
        userRepository.findById("k").ifPresent(ben -> {
//            ben.setSession(new UserSession("kjdlskdjflsdkfjsldkfjsldkf", new Date()));
            System.out.println(ben);
            userRepository.save(ben);
        });
    }
}
