package com.hotel.hotel.modules.notification.controller;

import com.hotel.hotel.modules.notification.dto.NotificationResponse;
import com.hotel.hotel.modules.notification.dto.NotificationUpdateDTO;
import com.hotel.hotel.modules.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @GetMapping("/{id}")
    public ResponseEntity getNotificationsUser(@PathVariable Long id) {
        List<NotificationResponse> notifications = service.getPendingsNotificationsUser(id);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{id}")
    public ResponseEntity updateNotification(@RequestBody NotificationUpdateDTO status, @PathVariable Long id) {
        service.updateStatus(id, status.status());
        return ResponseEntity.ok().build();
    }
}
