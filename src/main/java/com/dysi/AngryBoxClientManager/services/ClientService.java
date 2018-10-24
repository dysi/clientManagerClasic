package com.dysi.AngryBoxClientManager.services;

import java.util.List;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.models.Client;

public interface ClientService {
    public Client getClientFromUser(String username);
	public List<Client> getClients();
	public Client getClientByID(Long clientID);
	public String editClient(ClientDTO clientEdit);
	public List<ClientDTO> getClientsDTO();
}
