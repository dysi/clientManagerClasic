package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.UserCreateDTO;
import com.dysi.AngryBoxClientManager.models.Authority;
import com.dysi.AngryBoxClientManager.models.User;
import com.dysi.AngryBoxClientManager.repositories.AuthorityRepository;
import com.dysi.AngryBoxClientManager.repositories.UserRepository;
import com.dysi.AngryBoxClientManager.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthorityRepository authorityRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserCreateDTO getUserCreateDTOById(long id) {

        User u = this.userRepository.findOne(id);

        UserCreateDTO uDTO = new UserCreateDTO();
        uDTO.setId(u.getId());
        uDTO.setUsername(u.getUsername());
        uDTO.setPassword((u.getPassword()));
        uDTO.setFirstname(u.getFirstname());
        uDTO.setLastname(u.getLastname());
        uDTO.setEmail(u.getEmail());
        uDTO.setEnabled(u.getEnabled());

        List<Long> authorities = new ArrayList<>();
        for (Authority auth : u.getAuthorities()) {
            authorities.add(auth.getId());
        }

        uDTO.setAuthorities(authorities);

        return uDTO;

    }

    @Override
    public List<Authority> getAuthorities() {
        return this.authorityRepository.findAll();
    }

    @Override
    public User createUser(UserCreateDTO user) {
        User nu = new User();

        nu.setUsername(user.getUsername());
        nu.setFirstname(user.getFirstname());
        nu.setLastname(user.getLastname());
        nu.setEmail(user.getEmail());
        nu.setEnabled(user.getEnabled());
        nu.setPassword(passwordEncoder.encode(user.getPassword()));
        nu.setLastPasswordResetDate(new Date());
        nu.setAuthorities(this.getAuthorityList(user.getAuthorities()));

        return this.userRepository.saveAndFlush(nu);
    }

    @Override
    public User editUser(UserCreateDTO user) {
        User nu = this.userRepository.findOne(user.getId());

        nu.setUsername(user.getUsername());
        nu.setFirstname(user.getFirstname());
        nu.setLastname(user.getLastname());
        nu.setEmail(user.getEmail());
        nu.setEnabled(user.getEnabled());
        //nu.setPassword(passwordEncoder.encode(user.getPassword()));
        nu.setAuthorities(this.getAuthorityList(user.getAuthorities()));

        return this.userRepository.saveAndFlush(nu);
    }

    private List<Authority> getAuthorityList(List<Long> authIdList) {
        List<Authority> authList = new ArrayList<>();
        for (Long id : authIdList) {
            authList.add(this.authorityRepository.findOne(id));
        }
        return authList;
    }
}
