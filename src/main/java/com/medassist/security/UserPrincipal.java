package com.medassist.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserPrincipal {
    private String userId;
    private String clinicId;
    private String role;
}
