package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expresso.base.BasicEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "person")
@Inheritance(strategy = InheritanceType.JOINED)
// do not use this because with Hibernate 6.1, when you login with a user with a department, it fails
// org.hibernate.UnsupportedLockAttemptException: Lock mode not supported
// @Immutable
public class BasicPerson extends BasePerson implements BasicEntity<Person> {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id", insertable = false, updatable = false)
	private Person extendedPerson;

	@Override
	public Person getExtended() {
		return extendedPerson;
	}
}