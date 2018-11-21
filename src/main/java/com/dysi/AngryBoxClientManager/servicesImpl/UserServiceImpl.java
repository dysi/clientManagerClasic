package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.dtos.UserCreateDTO;
import com.dysi.AngryBoxClientManager.models.Authority;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.User;
import com.dysi.AngryBoxClientManager.repositories.AuthorityRepository;
import com.dysi.AngryBoxClientManager.repositories.ClientRepository;
import com.dysi.AngryBoxClientManager.repositories.UserRepository;
import com.dysi.AngryBoxClientManager.services.ClientService;
import com.dysi.AngryBoxClientManager.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AuthorityRepository authorityRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private ClientRepository clientRepository;

	@Override
	public UserCreateDTO getUserCreateDTOById(long id) {

		User u = this.userRepository.findOne(id);

		UserCreateDTO uDTO = new UserCreateDTO();
		uDTO.setId(u.getId());
		uDTO.setUsername(u.getUsername());
		// uDTO.setPassword((u.getPassword()));
		uDTO.setFirstname(u.getFirstname());
		uDTO.setLastname(u.getLastname());
		uDTO.setEmail(u.getEmail());
		uDTO.setEnabled(u.getEnabled());

		List<Long> authorities = new ArrayList<>();
		for (Authority auth : u.getAuthorities()) {
			authorities.add(auth.getId());
		}

		uDTO.setAuthorities(authorities);

		// Client client =
		// this.clientService.getClientFromUser(u.getUsername());
		uDTO.setClientID(u.getClient() != null ? u.getClient().getId() : null);

		return uDTO;

	}

	@Override
	public List<Authority> getAuthorities() {
		return this.authorityRepository.findAll();
	}

	@Override
	public User editUser(UserCreateDTO user) throws Exception {

		try {
			Client clientUpdate = null;

			User nu = new User();
			if (user.getId() != null && user.getId().longValue() > 0) {
				nu = this.userRepository.findOne(user.getId());
			}

			nu.setUsername(user.getUsername());

			nu.setFirstname(user.getFirstname());
			nu.setLastname(user.getLastname());

			nu.setEmail(user.getEmail());
			nu.setEnabled(user.getEnabled());
			nu.setPassword(passwordEncoder.encode(user.getPassword()));
			nu.setAuthorities(this.getAuthorityList(user.getAuthorities()));

			if (user.getClientID() != null && user.getClientID().longValue() > 0) {
				clientUpdate = this.clientRepository.findOne(user.getClientID());
				nu.setClient(clientUpdate);
				clientUpdate.setUser(nu);
			} else {
				nu.setClient(null);
				if (nu.getClient() != null) {
					clientUpdate = nu.getClient();
					clientUpdate.setUser(null);
				}
			}

			nu = this.userRepository.saveAndFlush(nu);

			if (clientUpdate != null) {
				this.clientRepository.save(clientUpdate);
			}

			return nu;
		} catch (Exception e) {
			throw new Exception("ERROR editUser: " + e.getMessage());
		}
	}

	private List<Authority> getAuthorityList(List<Long> authIdList) {
		List<Authority> authList = new ArrayList<>();
		for (Long id : authIdList) {
			authList.add(this.authorityRepository.findOne(id));
		}
		return authList;
	}

	@Override
	public List<UserCreateDTO> getAllUsersDTO() {

		List<UserCreateDTO> usuarios = this.userRepository.getAllUsersDTO();

		return (usuarios != null ? usuarios : new ArrayList<UserCreateDTO>());
	}
}
