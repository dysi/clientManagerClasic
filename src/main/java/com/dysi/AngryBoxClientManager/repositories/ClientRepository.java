package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.dtos.ClientLookup;
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
    
    @Query("select c from Client c order by c.lastname, c.firstname asc")
	public List<Client> findAllOrder();
    
    // Long id, String firstname, String lastname, String username, String companyName, Long companyId, Calendar initialActivity, Calendar lockDateC, boolean forcedLock
    @Query("select new com.dysi.AngryBoxClientManager.dtos.ClientLookup("
    		+ " c.id, c.firstname, c.lastname, u.username,"
    		+ " l.companyName, l.companyId, l.initialActivity, s.lockDate, s.forcedLock)"
    		+ " from Client c "
    		+ " left join c.licenses l "
    		+ " left join l.licenseStatuses s "
    		+ " left join c.user u "
    		+ " order by c.firstname asc, c.lastname asc")
	public List<ClientLookup> findAllLookup();
    
}
