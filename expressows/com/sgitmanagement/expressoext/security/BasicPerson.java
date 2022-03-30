package com.sgitmanagement.expressoext.security;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "person")
@Inheritance(strategy = InheritanceType.JOINED)
@Immutable
public class BasicPerson extends BasePerson {

}