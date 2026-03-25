package com.etisalat.controllers;

import com.etisalat.utils.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @GetMapping("/test")
    public ResponseEntity<?> me() {
        return ResponseEntity.ok(ResponseBuilder.success( "response "));
    }
}
