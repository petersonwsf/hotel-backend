package com.hotel.hotel.modules.rabbitmq.dtos;

import com.hotel.hotel.modules.user.model.Role;
import com.hotel.hotel.modules.user.model.User;

public record UserData(Long id, String name, String email, String phoneNumber, Role role) {
    public UserData(User user) {
        this(user.getId(), user.getName(), user.getLogin(), user.getPhoneNumber(), user.getRole());
    }
}