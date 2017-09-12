package com.dysi.AngryBoxClientManager.services;

import com.dysi.AngryBoxClientManager.dtos.UserCreateDTO;
import com.dysi.AngryBoxClientManager.models.Authority;
import com.dysi.AngryBoxClientManager.models.User;

import java.util.List;

public interface UserService {
    public UserCreateDTO getUserCreateDTOById(long id);

    public List<Authority> getAuthorities();

    public User createUser(UserCreateDTO user);

    public User editUser(UserCreateDTO user);
}
