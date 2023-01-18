package com.sgitmanagement.expressoext.security;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "person")
@Inheritance(strategy = InheritanceType.JOINED)
// do not use this because with Hibernate 6.1, when you login with a user with a department, it fails
// org.hibernate.UnsupportedLockAttemptException: Lock mode not supported
// @Immutable
public class BasicPerson extends BasePerson {

}