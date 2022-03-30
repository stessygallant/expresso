package com.sgitmanagement.expressoext.security;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import javax.persistence.EntityManager;

import com.sgitmanagement.expresso.base.PersistenceManager;

public class UserService extends BaseUserService<User> {

	public static void main(String[] args) throws Exception {
		PersistenceManager persistenceManager = PersistenceManager.getInstance();
		EntityManager em = persistenceManager.getEntityManager();

		UserService service = newServiceStatic(UserService.class, User.class);
		// service.process();
		// System.out.println(service.list());

		String fileName = "D:\\OneDrive\\Desktop\\user-roles.csv";
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.ISO_8859_1)) {
			writer.append("userName,fullName,role\n");
			for (User user : service.list(true)) {
				for (Role role : service.getAllRoles(user.getId())) {
					if (role.isSystemRole()) {
						// do not display
					} else {
						writer.append(user.getUserName() + ",\"" + user.getFullName() + "\"," + role.getPgmKey() + "\n");
					}
				}
			}
		}
		persistenceManager.commitAndClose(em);

		System.out.println("Done");
	}

}
