package com.hotel.hotel.modules.client.dtos;

import java.time.LocalDate;

import com.hotel.hotel.modules.client.model.Client;
import com.hotel.hotel.modules.contactInformation.dtos.ContactInformationDTO;

public record ClientDetailsDTO(Long id, String name, String pin, String email, LocalDate dateOfBirth, ContactInformationDTO contactInformation, Long userId, String imageKey) {
    public ClientDetailsDTO(Client client) {
        this(client.getId(),
            client.getName(),
            client.getPin(),
            client.getEmail(),
            client.getDateOfBirth(),
            new ContactInformationDTO(client.getContactInformation()),
            client.getUser().getId(),
            client.getUser().getProfilePicture());
    }
}
