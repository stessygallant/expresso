package com.sgitmanagement.expressoext.security;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "person")
@Inheritance(strategy = InheritanceType.JOINED)
@Immutable
public class BasicPerson extends BasePerson {

}