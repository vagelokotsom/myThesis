package com.thesis.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String email;
    private String password;
    private String username;
    private String role; // "ROLE_TEACHER" or "ROLE_STUDENT"
}
