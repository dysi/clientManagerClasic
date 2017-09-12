package com.dysi.AngryBoxClientManager.services;

import com.dysi.AngryBoxClientManager.models.Client;

public interface ClientService {
    public Client getClientFromUser(String username);
}
