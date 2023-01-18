package com.sgitmanagement.expressoext.file;

import java.io.File;
import java.util.List;

import com.sgitmanagement.expresso.dto.Query.Filter;
import com.sgitmanagement.expresso.exception.BaseException;
import com.sgitmanagement.expresso.exception.ForbiddenException;
import com.sgitmanagement.expresso.util.SystemEnv;
import com.sgitmanagement.expresso.util.Util;
import com.sgitmanagement.expressoext.base.BaseService;
import com.sgitmanagement.expressoext.security.Application;
import com.sgitmanagement.expressoext.security.ApplicationService;
import com.sgitmanagement.expressoext.security.AuthorizationHelper;

import jakarta.servlet.http.HttpServletResponse;

public class FileLinkService extends BaseService {

	public void downloadFile(String applicationName) throws Exception {
		// get the file from the application parameter
		Application application = newService(ApplicationService.class, Application.class).get(new Filter("pgmKey", applicationName));

		// verify if the user has access to the application
		List<Application> applications = AuthorizationHelper.getApplications(getUser());
		if (!applications.contains(application)) {
			throw new ForbiddenException("User [" + getUser().getUserName() + "] is not allowed to [" + applicationName + "] the file [" + application.getParameter() + "]");
		}

		String basePath = SystemEnv.INSTANCE.getProperties("config").getProperty("filelink") + File.separator;

		final File file = new File(basePath + application.getParameter());
		if (!file.exists()) {
			throw new BaseException(HttpServletResponse.SC_NOT_FOUND);
		}
		Util.downloadFile(getResponse(), file);
	}
}
