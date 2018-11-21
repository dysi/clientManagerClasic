package com.dysi.AngryBoxClientManager.controllers;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.dtos.UserCreateDTO;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.User;
import com.dysi.AngryBoxClientManager.services.ClientService;
import com.dysi.AngryBoxClientManager.services.UserService;

import java.util.List;

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
    @Autowired
    private ClientService clientService;
    
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
        
        List<UserCreateDTO> usuariosList = this.userService.getAllUsersDTO();
        
        List<ClientDTO> clientesList = this.clientService.getClientsDTO();
        
        model.addAttribute("clientesList", clientesList);
        model.addAttribute("usuariosList", usuariosList);
        model.addAttribute("authoritiesList", userService.getAuthorities());
        
        return "users/user-form";
        
    }

    @RequestMapping("/create/{id}")
    public String getEditUserPage(Model model, @PathVariable(value = "id") long id) {

        model.addAttribute("user", userService.getUserCreateDTOById(id));
        
        List<UserCreateDTO> usuariosList = this.userService.getAllUsersDTO();
        
        List<ClientDTO> clientesList = this.clientService.getClientsDTO();
        
        model.addAttribute("clientesList", clientesList);
        model.addAttribute("usuariosList", usuariosList);
        model.addAttribute("authoritiesList", userService.getAuthorities());
        
        return "users/user-form";
        
    }
    
    /**
     * Metodo para cambiar el usuario a editar desde el SELECT
     * @param model
     * @param userCreateDTO
     * @return
     */
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String updateUser(Model model, 
    		@ModelAttribute(value = "user") UserCreateDTO userCreateDTO) {

        try {
        	
        	User userEdit = userService.editUser(userCreateDTO);
            
            return this.getEditUserPage(model, userEdit.getId());

        } catch (Exception e) {
        	System.out.println("Error: UserController - /updateUser " + e.getMessage());
            return this.getCreateUserPage(model, userCreateDTO);
        }
    }

    /**
     * Metodo para cambiar el usuario a editar desde el SELECT
     * @param model
     * @param userCreateDTO
     * @return
     */
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public String changeForEdit(Model model, 
    		@ModelAttribute(value = "user") UserCreateDTO userCreateDTO) {

        try {
            
            return this.getEditUserPage(model, userCreateDTO.getId());

        } catch (Exception e) {
        	System.out.println("Error: UserController - /changeForEdit " + e.getMessage());
            return this.getCreateUserPage(model, userCreateDTO);
        }
    }
}
