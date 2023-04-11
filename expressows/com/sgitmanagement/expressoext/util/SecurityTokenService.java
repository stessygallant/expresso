package com.sgitmanagement.expressoext.util;

import java.util.Calendar;
import java.util.Date;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.dto.Query.Filter.Operator;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseEntityService;
import com.sgitmanagement.expressoext.security.User;

import jakarta.persistence.NoResultException;

public class SecurityTokenService extends BaseEntityService<SecurityToken> {

	public SecurityToken createNew(User user) throws Exception {
		String securityTokenNo = Util.generateRandomToken(50);
		return createNew(user, securityTokenNo);
	}

	/**
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public SecurityToken createNew(User user, String securityTokenNo) throws Exception {
		SecurityToken securityToken = new SecurityToken();
		securityToken.setSecurityTokenNo(securityTokenNo);
		securityToken.setCreationUserId(user.getId());
		securityToken.setCreationDate(new Date());
		return create(securityToken);
	}

	/**
	 * A security token is valid for 2 hours
	 *
	 * @param securityToken
	 * @return
	 * @throws Exception
	 */
	public SecurityToken get(String userName, String securityTokenNo) throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR_OF_DAY, -2);
		Date threasholdDate = calendar.getTime();

		// verify if the securityTokenNo is associated with the userName and it is still valid
		Filter filter = new Filter();
		filter.addFilter(new Filter("creationUser.userName", userName));
		filter.addFilter(new Filter("securityTokenNo", securityTokenNo));
		filter.addFilter(new Filter("creationDate", Operator.gt, threasholdDate));
		try {
			return get(filter);
		} catch (NoResultException ex) {
			logger.warn("Security token is not valid userName[" + userName + "] securityTokenNo[" + securityTokenNo + "] threasholdDate[" + threasholdDate + "]");
			return null;
		}
	}

	public boolean isValid(String userName, String securityTokenNo) throws Exception {
		return get(userName, securityTokenNo) != null;
	}
}
