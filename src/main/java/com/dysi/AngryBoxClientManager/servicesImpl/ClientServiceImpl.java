package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.dtos.ClientLookup;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.User;
import com.dysi.AngryBoxClientManager.repositories.ClientRepository;
import com.dysi.AngryBoxClientManager.repositories.UserRepository;
import com.dysi.AngryBoxClientManager.services.ClientService;

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
	public List<ClientLookup> getClientsLookups(String keyword, Integer statusId, Integer daysToLock) {
		if(keyword != null) {
			if(keyword.trim().length() == 0) {
				keyword = null;
			} else {
				keyword = ("%" + keyword.trim() + "%");
			}
		}

		if(statusId == null) {
			statusId = -1;
		}
		if(daysToLock == null) {
			daysToLock = 15;
		}
				
		Calendar dateMax = Calendar.getInstance();
		dateMax.add(Calendar.DAY_OF_MONTH, daysToLock);
		
		
		List<ClientLookup> clientsDTO = this.clientRepository.findAllLookupFilter(keyword, statusId, Calendar.getInstance(), dateMax);
		return clientsDTO;
	}

	@Override
	public Client getClientByID(Long clientID) {
		return this.clientRepository.findById(clientID).orElse(null);
	}

	@Override
	public String editClient(ClientDTO clientEdit) {
		Client cliente = new Client();

		if (clientEdit.getIdOld() > 0) {
			// EDICION Y ID CLIENTE SE MANTIENE
			cliente = this.clientRepository.findById(clientEdit.getIdOld()).orElse(null);
		}

		cliente.setLastname(clientEdit.getLastname() != null ? clientEdit.getLastname().trim() : "");
		cliente.setFirstname(clientEdit.getFirstname() != null ? clientEdit.getFirstname().trim() : "");
		cliente.setCuil(clientEdit.getCuil() != null ? clientEdit.getCuil().trim() : "");
		cliente.setPhone(clientEdit.getPhone() != null ? clientEdit.getPhone().trim() : "");
		cliente.setEmail(clientEdit.getEmail() != null ? clientEdit.getEmail().trim() : "");

		cliente = this.clientRepository.saveAndFlush(cliente);

		return "1|Cliente Creado Exitosamente";
	}

	@Override
	public List<ClientDTO> getClientsDTO() {
		List<ClientDTO> clients = this.clientRepository.findAllDTO();
		return (clients != null ? clients : new ArrayList<ClientDTO>());
	}
}
