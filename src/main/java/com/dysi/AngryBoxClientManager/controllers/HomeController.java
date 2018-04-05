package com.dysi.AngryBoxClientManager.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/index")
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
