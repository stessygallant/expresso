package com.sgitmanagement.expressoext.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import com.sgitmanagement.expressoext.base.BaseService;

public class SupportService extends BaseService {
	static private Stack<String> lastMails = new Stack<>();

	public void sendSupportEmail(String title, String message) throws Exception {
		// only send the same email once (avoid sending email in a loop)
		if (!lastMails.contains(message)) {
			lastMails.push(message);
			if (lastMails.size() > 100) {
				lastMails.pop();
			}
			logger.warn(title + " - " + message);
			// Mailer.INSTANCE.sendMail(title, message);
		}
	}

	public List<String> getPrinterNames() throws Exception {
		List<String> printerNames = new ArrayList<>();
		PrintService[] pss = PrintServiceLookup.lookupPrintServices(null, null);
		for (PrintService ps : pss) {
			printerNames.add(ps.getName());
		}
		return printerNames;
	}
}
