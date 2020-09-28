package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.dtos.ClientLookup;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.License;
import com.dysi.AngryBoxClientManager.models.User;
import com.dysi.AngryBoxClientManager.repositories.ClientRepository;
import com.dysi.AngryBoxClientManager.repositories.UserRepository;
import com.dysi.AngryBoxClientManager.services.ClientService;
import com.dysi.AngryBoxClientManager.utils.FechasHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientServiceImpl implements ClientService {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ClientRepository clientRepository;

	@Override
	public Client getClientFromUser(String username) {
		User user = this.userRepository.findByUsername(username);
		return user.getClient();
	}

	@Override
	public List<ClientDTO> getClients() {
		List<Client> clients = this.clientRepository.findAll();
		List<ClientDTO> clientsDTO = new ArrayList<ClientDTO>();

		if(clients != null){
			for (Client c : clients) {
				ClientDTO cDTO = new ClientDTO(c.getId(), c.getFirstname(), c.getLastname(), c.getEmail(), c.getPhone(),
						c.getCuil(), (c.getUser() != null ? c.getUser().getUsername() : "SIN ASIGNAR"));
				clientsDTO.add(cDTO);
			}
		}

		return clientsDTO;
	}
	
	@Override
	public List<ClientLookup> getClientsLookups() {
//		List<Client> clients = this.clientRepository.findAllOrder();
//		List<ClientLookup> clientsDTO = new ArrayList<ClientLookup>();
//
//		if(clients != null){
//			Calendar now = Calendar.getInstance();
//			
//			for (Client c : clients) {
//				
//				Long id = c.getId();
//				
//			    String firstname = c.getFirstname();
//			    String lastname = c.getLastname();
//			    String email = c.getEmail();
//			    String phone = c.getPhone();
//			    String cuil = c.getCuil();
//			    String username = (c.getUser() != null ? c.getUser().getUsername() : "SIN ASIGNAR");
//				
//			    if(c.getLicenses() != null && c.getLicenses().size() > 0) {
//			    	for (License l : c.getLicenses()) {
//			    		String lockDate = "NO IDENTIFICADO";
//			    		String block = "";
//			    		if(l.getLicenseStatuses() != null && l.getLicenseStatuses().size() > 0) {
//			    			Calendar lockDateAux = l.getLicenseStatuses().get(0).getLockDate();
//			    			lockDate = FechasHelper.getStringFromCalendarForSimplePrint(lockDateAux);
//			    			block = l.getLicenseStatuses().get(0).isForcedLock() ? "FORZADO" : ((lockDateAux != null && lockDateAux.getTimeInMillis() <= now.getTimeInMillis()) ? "VENCIDA" : "");
//			    		}
//			    		
//			    		ClientLookup cDTO = new ClientLookup(id, firstname, lastname, email, phone, cuil, username, 
//			    				l.getDescription(), l.getCompanyName(), l.getCompanyId(), 
//			    				FechasHelper.getStringFromCalendarForSimplePrint(l.getInitialActivity()), 
//			    				lockDate, l.isPrePayment(), block);
//						clientsDTO.add(cDTO);
//					}
//			    } else {
//			    	ClientLookup cDTO = new ClientLookup(id, firstname, lastname, email, phone, cuil, username, "", "", -1l, "", "", false, "");
//					clientsDTO.add(cDTO);
//			    }
//				
//			}
//		}

		List<ClientLookup> clientsDTO = this.clientRepository.findAllLookup();
		return clientsDTO;
	}

	@Override
	public Client getClientByID(Long clientID) {
		return this.clientRepository.findOne(clientID);
	}

	@Override
	public String editClient(ClientDTO clientEdit) {
		Client cliente = new Client();

		if (clientEdit.getIdOld() > 0) {
			// EDICION Y ID CLIENTE SE MANTIENE
			cliente = this.clientRepository.findOne(clientEdit.getIdOld());
		}
		// else {
		// //NUEVO
		// Client clienteExiste =
		// this.clientRepository.findOne(clientEdit.getId());
		// if(clienteExiste != null){
		// return ("99|EL ID YA ESTA EN USO: #" + clienteExiste.getId() + ". " +
		// clienteExiste.getFirstname() + " " + clienteExiste.getLastname());
		// }
		// cliente.setId(clientEdit.getId());
		// }

		cliente.setLastname(clientEdit.getLastname().trim());
		cliente.setFirstname(clientEdit.getFirstname().trim());
		cliente.setCuil(clientEdit.getCuil().trim());
		cliente.setPhone(clientEdit.getPhone().trim());
		cliente.setEmail(clientEdit.getEmail().trim());

		cliente = this.clientRepository.saveAndFlush(cliente);

		return "1|Cliente Creado Exitosamente";
	}

	@Override
	public List<ClientDTO> getClientsDTO() {
		List<ClientDTO> clients = this.clientRepository.findAllDTO();
		return (clients != null ? clients : new ArrayList<ClientDTO>());
	}
}
