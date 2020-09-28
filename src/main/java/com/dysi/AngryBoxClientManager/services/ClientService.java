package com.dysi.AngryBoxClientManager.services;

import java.util.List;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.dtos.ClientLookup;
import com.dysi.AngryBoxClientManager.models.Client;

public interface ClientService {
    public Client getClientFromUser(String username);
	public List<ClientDTO> getClients();
	public Client getClientByID(Long clientID);
	public String editClient(ClientDTO clientEdit);
	public List<ClientDTO> getClientsDTO();
	public List<ClientLookup> getClientsLookups();
}
