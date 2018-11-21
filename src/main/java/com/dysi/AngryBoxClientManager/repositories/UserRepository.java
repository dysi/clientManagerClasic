package com.dysi.AngryBoxClientManager.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dysi.AngryBoxClientManager.dtos.UserCreateDTO;
import com.dysi.AngryBoxClientManager.models.User;
import org.springframework.data.jpa.repository.Query;

/**
 * Created by stephan on 20.03.16.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    @Query("select new com.dysi.AngryBoxClientManager.dtos.UserCreateDTO("
    		+ " u.id, u.username, u.firstname, u.lastname)"
    		+ " from User u order by u.firstname asc, u.lastname asc")
	List<UserCreateDTO> getAllUsersDTO();

}
