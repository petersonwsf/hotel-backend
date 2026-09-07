package com.hotel.hotel.modules.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.hotel.config.exceptions.ResourceAlreadyExists;
import com.hotel.hotel.config.exceptions.ResourceNotFoundException;
import com.hotel.hotel.modules.audit.AuditService;
import com.hotel.hotel.modules.audit.Auditable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.hotel.hotel.modules.client.dtos.ClientEditDTO;
import com.hotel.hotel.modules.client.dtos.ClientFilter;
import com.hotel.hotel.modules.client.dtos.ClientSaveDTO;
import com.hotel.hotel.modules.client.model.Client;
import com.hotel.hotel.modules.client.repository.ClientRepository;
import com.hotel.hotel.modules.client.repository.specs.ClientSpecification;
import com.hotel.hotel.modules.files.service.FileService;
import com.hotel.hotel.modules.user.dtos.UserSaveDTO;
import com.hotel.hotel.modules.user.model.Role;
import com.hotel.hotel.modules.user.model.User;
import com.hotel.hotel.modules.user.service.UserService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ClientService {

    @Autowired
    private ClientRepository repository;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    private FileService fileService;


    @Auditable(action = "CLIENT_CREATE", resourceType = "CLIENT")
    @Transactional
    public Client create(ClientSaveDTO data) {
        log.info("Starting process to create client {}", data.name());

        verifyEmailExists(data.email(), null);
        verifyPhoneNumberExists(data.contactInformation().phoneNumber(), null);
        verifyPinExists(data.pin(), null);

        UserSaveDTO newUser = new UserSaveDTO(data.name(), data.email(), data.password(), data.contactInformation().phoneNumber(), Role.CLIENT);
        User user = userService.register(newUser);

        log.info("The client user {} was successfully created", data.name());
        Client client = new Client(data, user);
        Client newClient = repository.save(client);

        log.info("The client {} was successfully created", data.name());
        return newClient;
    }

    public Page<Client> list(ClientFilter filter, Pageable pagination) {
        log.info("Starting process to list clients");
        Specification<Client> filters = (root, query, callBack) -> null;
        filters = filters
            .and(ClientSpecification.nameLike(filter.name()))
            .and(ClientSpecification.pinEqual(filter.pin()))
            .and(ClientSpecification.emailEqual(filter.email()))
            .and(ClientSpecification.phoneNumberEqual(filter.phoneNumber()))
            .and(ClientSpecification.isDeleted(filter.deleted()));
        log.info("Returning the clients list");
        return repository.findAll(filters, pagination);
    }

    @Transactional
    @PreAuthorize("@securityHelper.hasClientPermission(#id)")
    public Client edit(ClientEditDTO data, Long id) {
        log.info("Starting process to edit client eith ID: {}", id);
        Client client = getById(id);
        User user = userService.findById(client.getUser().getId());
        String beforeUpdate = null;
        try {
            beforeUpdate = objectMapper.writeValueAsString(client);
        } catch (JsonProcessingException e) {
            log.warn("Erro ao processar JSON para auditoria " + e);
        }
        if (data.email() != null) {
            verifyEmailExists(data.email(), id);
        }
        if (data.contactInformation() != null && data.contactInformation().phoneNumber() != null) {
            verifyPhoneNumberExists(data.contactInformation().phoneNumber(), id);
        }
        updateUserByClient(user, data.name(), data.email(), data.contactInformation().phoneNumber());
        client.edit(data);
        auditService.recordUpdate("CLIENT_UPDATE", "CLIENT", String.valueOf(id), beforeUpdate, client);
        log.info("Client with ID: {} successfully edited"); 
        return client;
    }

    @Auditable(action = "CLIENT_DELETE", resourceType = "CLIENT")
    @Transactional
    @PreAuthorize("@securityHelper.hasClientPermission(#id)")
    public void deleteById(Long id) {
        Client client = getById(id);
        client.delete();
    }

    @Auditable(action = "CLIENT_GET_BY_ID", resourceType = "CLIENT")
    @PreAuthorize("@securityHelper.hasClientPermission(#id)")
    public Client getById(Long id) {
        Client client = repository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Client with id " + id + " does not exists"));
        return client;
    }

    @Auditable(action = "CLIENT_GET_BY_USER_ID", resourceType = "CLIENT")
    @PreAuthorize("@securityHelper.hasClientUserPermission(#id)")
    public Client getClientByUserId(Long id) {
        Client client = repository.findByUserId(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Client with user id " + id + " does not exists"));
        return client;
    }

    private void verifyEmailExists(String email, Long id) {
        Boolean emailExists = (id != null)
                ? repository.existsByEmailAndIdNot(email, id)
                : repository.existsByEmail(email);
        if (emailExists) throw new ResourceAlreadyExists("Email já está sendo utilizado, tente outro");
    }

    private void verifyPinExists(String pin, Long id) {
        Boolean pinExists = (id != null)
                ? repository.existsByPinAndIdNot(pin, id)
                : repository.existsByPin(pin);
        if (pinExists) throw new ResourceAlreadyExists("CPF já está sendo utilizado, tente outro");
    }

    private void verifyPhoneNumberExists(String phoneNumber, Long id) {
        Boolean phoneNumberExists = (id != null)
                ? repository.existsByContactInformation_PhoneNumberAndIdNot(phoneNumber, id)
                : repository.existsByContactInformation_PhoneNumber(phoneNumber);
        if (phoneNumberExists) throw new ResourceAlreadyExists("Número de telefone já está sendo utilizado, tente outro");
    }

    @Transactional
    private void updateUserByClient(User user, String name, String email, String phoneNumber) {
        if (email != null) {
            user.setLogin(email);
        }
        if (name != null) {
            user.setName(name);
        }
        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber);
        }
    }
}
