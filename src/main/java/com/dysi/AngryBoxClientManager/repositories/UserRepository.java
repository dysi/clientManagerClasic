package com.dysi.AngryBoxClientManager.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dysi.AngryBoxClientManager.dtos.UserCreateDTO;
import com.dysi.AngryBoxClientManager.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Created by stephan on 20.03.16.
 */
public interface UserRepository extends JpaRepository<User, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {
    User findByUsername(String username);
    List<User> findByClient(com.dysi.AngryBoxClientManager.models.Client client);

    interface ClientUserCount {
        Long getClientId();
        long getTotal();
    }

    @Query("select u.client.id as clientId, count(u) as total from User u where u.client.id in :ids group by u.client.id")
    List<ClientUserCount> countForClients(@Param("ids") java.util.Collection<Long> ids);

    @Query("select new com.dysi.AngryBoxClientManager.dtos.UserCreateDTO("
    		+ " u.id, u.username, u.firstname, u.lastname)"
    		+ " from User u order by u.firstname asc, u.lastname asc")
	List<UserCreateDTO> getAllUsersDTO();
    
    @Query("select u from User u where u.username = :username")
	List<User> existUser(@Param("username") String username);

}
