package com.dysi.AngryBoxClientManager.security;

import java.util.ArrayList;
import java.util.List;
//import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.dysi.AngryBoxClientManager.models.Authority;
import com.dysi.AngryBoxClientManager.models.User;

public final class JwtUserFactory {

    private JwtUserFactory() {
    }

    public static JwtUser create(User user) {
        return new JwtUser(
                user.getId(),
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getPassword(),
                mapToGrantedAuthorities(user.getAuthorities()),
                user.getEnabled(),
                user.getLastPasswordResetDate()
        );
    }

    private static List<GrantedAuthority> mapToGrantedAuthorities(List<Authority> authorities) {
//        return authorities.stream()
//                .map(authority -> new SimpleGrantedAuthority(authority.getName().name()))
//                .collect(Collectors.toList());

        List<GrantedAuthority>  grantedAuthorities = new ArrayList<>();

        for(Authority authority: authorities){
            SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(authority.getName().name());
            grantedAuthorities.add(simpleGrantedAuthority);
        }

        return grantedAuthorities;
    }
}
