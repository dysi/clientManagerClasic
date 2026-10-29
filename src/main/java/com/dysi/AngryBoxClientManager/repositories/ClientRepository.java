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
    @Query("select new com.dysi.AngryBoxClientManager.dtos.ClientSyncIdentity("
            + "c.id, c.firstname, c.lastname, c.legalName, c.tradeName, c.email, c.phone, "
            + "c.dni, c.cuil, c.externalCode, c.removeDate) from Client c order by c.id")
    List<com.dysi.AngryBoxClientManager.dtos.ClientSyncIdentity> findSyncIdentities();

    String NORMALIZED_CUIT = "replace(replace(replace(coalesce(c.cuil, ''), '-', ''), '.', ''), ' ', '')";
    // REPLACE funciona también en servidores sin REGEXP_REPLACE. Un resto no vacío
    // identifica datos históricos con letras u otros caracteres, que no deben autenticar.
    String CUIT_WITHOUT_DIGITS = "replace(replace(replace(replace(replace("
            + "replace(replace(replace(replace(replace(" + NORMALIZED_CUIT
            + ", '0', ''), '1', ''), '2', ''), '3', ''), '4', ''),"
            + " '5', ''), '6', ''), '7', ''), '8', ''), '9', '')";

    @Query(value = "select c from Client c left join fetch c.user u left join ClientAccountBalance b on b.client = c and b.removeDate is null where ("
            + "lower(concat(c.firstname, ' ', c.lastname)) like :keyword escape '!' "
            + "or lower(concat(c.lastname, ' ', c.firstname)) like :keyword escape '!' "
            + "or lower(coalesce(c.email, '')) like :keyword escape '!' "
            + "or lower(coalesce(c.legalName, '')) like :keyword escape '!' "
            + "or lower(coalesce(c.tradeName, '')) like :keyword escape '!' "
            + "or lower(coalesce(c.cuil, '')) like :keyword escape '!' "
            + "or lower(coalesce(u.username, '')) like :keyword escape '!')"
            + " and (:balanceState = 'ALL' or (:balanceState = 'UNKNOWN' and b.id is null)"
            + " or (:balanceState = 'DEBT' and b.amount > 0)"
            + " or (:balanceState = 'CREDIT' and b.amount < 0)"
            + " or (:balanceState = 'ZERO' and b.amount = 0))"
            + " and (:clientStatus = 'ALL' or (:clientStatus = 'ACTIVE' and c.removeDate is null)"
            + " or (:clientStatus = 'REMOVED' and c.removeDate is not null))"
            + " and (:staleBefore is null or b.synchronizedAt < :staleBefore)",
            countQuery = "select count(c) from Client c left join c.user u left join ClientAccountBalance b on b.client = c and b.removeDate is null where ("
            + "lower(concat(c.firstname, ' ', c.lastname)) like :keyword escape '!' "
            + "or lower(concat(c.lastname, ' ', c.firstname)) like :keyword escape '!' "
            + "or lower(coalesce(c.email, '')) like :keyword escape '!' "
            + "or lower(coalesce(c.legalName, '')) like :keyword escape '!' "
            + "or lower(coalesce(c.tradeName, '')) like :keyword escape '!' "
            + "or lower(coalesce(c.cuil, '')) like :keyword escape '!' "
            + "or lower(coalesce(u.username, '')) like :keyword escape '!')"
            + " and (:balanceState = 'ALL' or (:balanceState = 'UNKNOWN' and b.id is null)"
            + " or (:balanceState = 'DEBT' and b.amount > 0)"
            + " or (:balanceState = 'CREDIT' and b.amount < 0)"
            + " or (:balanceState = 'ZERO' and b.amount = 0))"
            + " and (:clientStatus = 'ALL' or (:clientStatus = 'ACTIVE' and c.removeDate is null)"
            + " or (:clientStatus = 'REMOVED' and c.removeDate is not null))"
            + " and (:staleBefore is null or b.synchronizedAt < :staleBefore)")
    org.springframework.data.domain.Page<Client> searchForWeb(@Param("keyword") String keyword,
            @Param("balanceState") String balanceState,
            @Param("clientStatus") String clientStatus,
            @Param("staleBefore") java.time.LocalDateTime staleBefore,
            org.springframework.data.domain.Pageable pageable);
	
    default org.springframework.data.domain.Page<Client> searchForWeb(String keyword,
            org.springframework.data.domain.Pageable pageable) {
        return searchForWeb(keyword, "ALL", "ALL", null, pageable);
    }

    public Client findClientByUser(User user);

    // Serializa presentaciones de un cliente para resolver reenvíos sin duplicar evidencia.
    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Client c where c.id = :id")
    java.util.Optional<Client> lockPortalClient(@Param("id") Long id);

    // Filtrar y limitar en BD; no cargar el padrón de documentos en memoria.
    @Query("select c.id from Client c where "
            + "((:type = 'CUIT' and " + NORMALIZED_CUIT + " = :document) "
            + "or (:type = 'DNI' and ((length(c.dni) > 0 and function('lpad', c.dni, 8, '0') = :dni) "
            + "or (length(" + NORMALIZED_CUIT + ") = 11 and length(" + CUIT_WITHOUT_DIGITS + ") = 0 "
            + "and substring(" + NORMALIZED_CUIT + ", 3, 8) = :dni))))")
    List<Long> findPortalDocumentMatches(@Param("type") String type, @Param("document") String document,
            @Param("dni") String dni, org.springframework.data.domain.Pageable pageable);

    @Query("select c.id from Client c where c.user.id = :userId")
    List<Long> findPortalClientsForUser(@Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    @Query("select new com.dysi.AngryBoxClientManager.dtos.ClientDTO("
            + " c.id, c.firstname, c.lastname, c.email, c.phone, c.cuil, u.username, c.legalName, c.tradeName, c.externalCode)"
    		+ " from Client c "
    		+ " left join c.user u "
    		+ " order by c.firstname asc, c.lastname asc")
	public List<ClientDTO> findAllDTO();
    
    @Query("select c from Client c order by c.lastname, c.firstname asc")
	public List<Client> findAllOrder();
    
    // Long id, String firstname, String lastname, String username, String companyName, Long companyId, Calendar initialActivity, Calendar lockDateC, boolean forcedLock
    @Query("select new com.dysi.AngryBoxClientManager.dtos.ClientLookup("
            + " c.id, c.firstname, c.lastname, c.email, c.phone, c.cuil, u.username,"
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
            + " c.id, c.firstname, c.lastname, c.email, c.phone, c.cuil, u.username,"
    		+ " l.companyName, l.companyId, l.initialActivity, s.lockDate, s.forcedLock)"
    		+ " from Client c "
    		+ " left join c.licenses l "
    		+ " left join l.licenseStatuses s "
    		+ " left join c.user u "
    		+ " where (:keyword is null "
    		+ "    or (lower(l.companyName) like lower(:keyword)) "
    		+ "    or (lower(concat(c.firstname,' ', c.lastname)) like lower(:keyword))"
    		+ "    or (lower(concat(c.lastname,' ', c.firstname)) like lower(:keyword))"
            + "    or (lower(c.legalName) like lower(:keyword))"
            + "    or (lower(c.tradeName) like lower(:keyword))"
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
