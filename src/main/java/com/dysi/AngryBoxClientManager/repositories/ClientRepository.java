package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
	
    public Client findClientByUser(User user);


    @Query("select new com.dysi.AngryBoxClientManager.dtos.ClientDTO("
    		+ " c.id, c.firstname, c.lastname, c.email, c.phone, c.cuil, u.username)"
    		+ " from Client c "
    		+ " left join c.user u "
    		+ " order by c.firstname asc, c.lastname asc")
	public List<ClientDTO> findAllDTO();
}
