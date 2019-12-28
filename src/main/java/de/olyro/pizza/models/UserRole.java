package de.olyro.pizza.models;

import io.javalin.core.security.Role;

public enum UserRole implements Role {
    ANYONE, ADMIN
}