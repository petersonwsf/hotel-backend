package com.hotel.hotel.modules.client.controller;

import com.hotel.hotel.modules.user.dtos.UserJsonDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.hotel.hotel.config.exceptions.MessageResponse;
import com.hotel.hotel.config.security.TokenService;
import com.hotel.hotel.modules.client.dtos.ClientDetailsDTO;
import com.hotel.hotel.modules.client.dtos.ClientEditDTO;
import com.hotel.hotel.modules.client.dtos.ClientFilter;
import com.hotel.hotel.modules.client.dtos.ClientListDTO;
import com.hotel.hotel.modules.client.dtos.ClientSaveDTO;
import com.hotel.hotel.modules.client.model.Client;
import com.hotel.hotel.modules.client.service.ClientService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/client")
public class ClientController {

    @Autowired
    private ClientService service;

    @Autowired
    private TokenService tokenService;

    @PostMapping
    public ResponseEntity create(@RequestBody @Valid ClientSaveDTO data, UriComponentsBuilder uriBuilder) {
        log.info("Received a request to create client named {}", data.name());
        Client client = service.create(data);
        var uri = uriBuilder.path("/client/{id}").buildAndExpand(client.getId()).toUri();
        log.info("Client {} was successfully created", data.name());
        var token = tokenService.createToken(client.getUser());
        return ResponseEntity.created(uri).body(new UserJsonDTO(token));
    }

    @GetMapping
    public ResponseEntity<Page<ClientListDTO>> list(ClientFilter filters, Pageable pagination) {
        log.info("Received a request to list clients");
        var clients = service.list(filters, pagination).map(ClientListDTO::new);
        log.info("Responding a request to list clients");
        return ResponseEntity.ok(clients);
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity edit(@RequestBody @Valid ClientEditDTO data, @PathVariable Long id) {
        log.info("Received a request to edit client with ID {}", id);
        Client client = service.edit(data, id);
        log.info("Client with ID: {} was successfully edited", id);
        return ResponseEntity.ok(new ClientDetailsDTO(client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable Long id) {
        log.info("Received a request to delete client with ID {}", id);
        service.deleteById(id);
        log.info("Client with ID: {} was successfully deleted", id);
        return ResponseEntity.ok(new MessageResponse("Client deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity getClientById(@PathVariable Long id) {
        log.info("Received a request to retrieve client with ID {}", id);
        Client client = service.getById(id);
        log.info("Client with ID: {} successfully retrieved", id);
        return ResponseEntity.ok(new ClientDetailsDTO(client));
    }

    @GetMapping("/user/{id}")
    public ResponseEntity getClientByUserId(@PathVariable Long id) {
        log.info("Received a request to retrieve client with id user {}", id);
        Client client = service.getClientByUserId(id);
        log.info("Client with id user: {} successfully retrieved", id);
        return ResponseEntity.ok(new ClientDetailsDTO(client));
    }
}