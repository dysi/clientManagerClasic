package com.dysi.AngryBoxClientManager.controllers;

import com.dysi.AngryBoxClientManager.dtos.UserCreateDTO;
import com.dysi.AngryBoxClientManager.services.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Value("${server.port}")
    private int port;

    @RequestMapping("/login")
    public String getLoginUserPage(Model model, String message) {
    	
    	int currentPort = this.port;
        model.addAttribute("message", message);
        model.addAttribute("port", this.port);
        return "users/login";
        
    }

    @RequestMapping("/create")
    public String getCreateUserPage(Model model, UserCreateDTO userCreateDTO) {

        UserCreateDTO user = userCreateDTO != null ? userCreateDTO : new UserCreateDTO();
        model.addAttribute("user", user);
        model.addAttribute("authoritiesList", userService.getAuthorities());
        return "users/user-form";
        
    }

    @RequestMapping("/create/{id}")
    public String getEditUserPage(Model model, @PathVariable(value = "id") long id) {

        model.addAttribute("user", userService.getUserCreateDTOById(id));
        model.addAttribute("authoritiesList", userService.getAuthorities());
        return "users/user-form";
        
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String processForm(Model model, 
    		@ModelAttribute(value = "user") UserCreateDTO userCreateDTO) {

        try {
            if (userCreateDTO.getId() != null) {
                // llamar service
                this.userService.editUser(userCreateDTO);
                return this.getLoginUserPage(model, "Usuario editado con éxito");
            }

            this.userService.createUser(userCreateDTO);
            return this.getLoginUserPage(model,"Usuario creado con éxito");

        } catch (Exception e) {
            return this.getCreateUserPage(model, userCreateDTO);
        }
        
    }
}
