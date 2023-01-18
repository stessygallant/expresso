package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseEntityService;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class PrivilegeService extends BaseEntityService<Privilege> {
	public Privilege get(int actionId, int resourceId) {
		// find the privilege with the action and resource
		TypedQuery<Privilege> q = getEntityManager().createQuery("SELECT p FROM Privilege p WHERE p.actionId = :actionId AND p.resourceId = :resourceId", Privilege.class)
				.setParameter("actionId", actionId).setParameter("resourceId", resourceId);
		try {
			return q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
}
