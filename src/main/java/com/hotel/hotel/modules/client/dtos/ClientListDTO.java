package com.hotel.hotel.modules.client.dtos;

import java.time.LocalDate;

import com.hotel.hotel.modules.client.model.Client;
import com.hotel.hotel.modules.contactInformation.dtos.ContactInformationDTO;
import com.hotel.hotel.modules.user.dtos.UserResponseDTO;

public record ClientListDTO(Long id, String name, String email, String pin, LocalDate dateOfBirth, ContactInformationDTO contactInformation, UserResponseDTO user) {
    public ClientListDTO(Client client) {
        this(client.getId(), client.getName(), client.getEmail(), client.getPin(), client.getDateOfBirth(), new ContactInformationDTO(client.getContactInformation()), new UserResponseDTO(client.getUser()));
    }
}
