package com.dysi.AngryBoxClientManager.controllers;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;

import com.dysi.AngryBoxClientManager.models.User;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
	
	@RequestMapping("/home")
	public String getHome(){
		return "home/home";
	}
	
//	@RequestMapping("/redirect-to")
//	public String addUser(HttpServletResponse response,
//			@RequestParam(value = "url", required = true) String url,
//			@RequestParam(value = "token", required = true) String token)
//	  {
//	      response.setHeader("Authorization","Bearer " + token);
//	      return "redirect:/" + url;
//	  }
	
}
