package com.zgamelogic.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping({
            "/",
            "/create",
            "/view/**",
            "/login/callback",
            "/users"
    })
    private String frontEnd(){
        return "forward:/index.html";
    }
}
