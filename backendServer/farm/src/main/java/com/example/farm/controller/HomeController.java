package com.example.farm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/farm/save")
    public String save() {
        return "save";
    }
    @GetMapping("/farm/login")
    public String login() {
        return "login";
    }
    @GetMapping("/farm/findId")
    public String findId() {
        return "findId";
    }
    @GetMapping("/farm/findPassword")
    public String findPassword() {
        return "findPassword";
    }
    @GetMapping("/")
    public String index() {
        return "index";
    }
}