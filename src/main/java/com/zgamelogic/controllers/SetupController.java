package com.zgamelogic.controllers;

import com.zgamelogic.data.database.configuration.ConfigurationItemRepository;
import com.zgamelogic.data.database.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SetupController {

    private final ConfigurationItemRepository configurationItemRepository;
    private final UserRepository userRepository;

    @Autowired
    public SetupController(ConfigurationItemRepository configurationItemRepository, UserRepository userRepository) {
        this.configurationItemRepository = configurationItemRepository;
        this.userRepository = userRepository;
    }
}
