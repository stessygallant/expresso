package com.sgitmanagement.expressoext.security;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import com.sgitmanagement.expressoext.util.MainUtil;

public class UserService extends BaseUserService<User> {

	public void printRoles() throws Exception {
		String fileName = "D:\\OneDrive\\Desktop\\user-roles.csv";
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.ISO_8859_1)) {
			writer.append("userName,fullName,role\n");
			for (User user : list(true)) {
				for (Role role : getAllRoles(user.getId())) {
					if (role.isSystemRole()) {
						// do not display
					} else {
						writer.append(user.getUserName() + ",\"" + user.getFullName() + "\"," + role.getPgmKey() + "\n");
					}
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		UserService service = newServiceStatic(UserService.class, User.class);
		service.list();

		MainUtil.close();
	}
}
