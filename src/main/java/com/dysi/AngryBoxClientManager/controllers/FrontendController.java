package com.dysi.AngryBoxClientManager.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FrontendController {
    @GetMapping({"/", "/ui"})
    public String entry() { return "redirect:/ui/"; }

    @GetMapping("/ui/")
    public String application() { return "forward:/ui/index.html"; }

    @GetMapping("/users/login")
    public String login() { return "redirect:/ui/#/login"; }

    @GetMapping("/users/create")
    public String newUser() { return "redirect:/ui/#/users/new"; }

    @GetMapping({"/users/create/{id}", "/users/edit/{id}"})
    public String editUser(@PathVariable long id) { return "redirect:/ui/#/users/" + id + "/edit"; }

    @GetMapping("/index/home")
    public String oldHome() { return "redirect:/ui/#/clients"; }
}
