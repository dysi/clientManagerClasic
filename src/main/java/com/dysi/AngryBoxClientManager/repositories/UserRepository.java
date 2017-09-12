package com.dysi.AngryBoxClientManager.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dysi.AngryBoxClientManager.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Created by stephan on 20.03.16.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

}
