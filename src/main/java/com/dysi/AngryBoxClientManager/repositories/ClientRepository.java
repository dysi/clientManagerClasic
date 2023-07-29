package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.dtos.ClientLookup;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.User;

import java.util.Calendar;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
	/**	
	<option value="-1">TODAS</option>
	<option value="1">ACTIVAS</option>
	<option value="2">POR VENCER</option>
	<option value="88">VENCIDAS</option>
	<option value="99">BLOQUEADAS</option>
	 */
    
    @Query("select new com.dysi.AngryBoxClientManager.dtos.ClientLookup("
    		+ " c.id, c.firstname, c.lastname, u.username,"
    		+ " l.companyName, l.companyId, l.initialActivity, s.lockDate, s.forcedLock)"
    		+ " from Client c "
    		+ " left join c.licenses l "
    		+ " left join l.licenseStatuses s "
    		+ " left join c.user u "
    		+ " where (:keyword is null "
    		+ "    or (lower(l.companyName) like lower(:keyword)) "
    		+ "    or (lower(concat(c.firstname,' ', c.lastname)) like lower(:keyword))"
    		+ "    or (lower(concat(c.lastname,' ', c.firstname)) like lower(:keyword))"
    		+ " )"
    		+ " and (:statusId is null or :statusId = -1 "
    		+ "    or (:statusId = 1 and s.forcedLock = false and s.lockDate > :dateNow)"
    		+ "    or (:statusId = 2 and s.forcedLock = false and s.lockDate > :dateNow and s.lockDate <= :dateMax)"
    		+ "    or (:statusId = 88 and s.forcedLock = false and s.lockDate < :dateNow)"
    		+ "    or (:statusId = 99 and s.forcedLock = true)"
    		+ " ) "
    		+ " order by c.firstname asc, c.lastname asc")
	public List<ClientLookup> findAllLookupFilter(
			@Param("keyword") String keyword, 
			@Param("statusId") Integer statusId,
			@Param("dateNow") Calendar dateNow,
			@Param("dateMax") Calendar dateMax);
    
}
