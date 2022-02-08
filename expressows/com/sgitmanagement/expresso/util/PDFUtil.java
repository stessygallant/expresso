package com.sgitmanagement.expresso.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;

/**
 * 
 */
public class PDFUtil {
	public static File createPDFFromHTML(String pdfFilenName, File htmlFile) throws Exception {
		// Create PDF from HTML
		// https://gitee.com/chqiuu/openhtmltopdf
		// https://gitee.com/chqiuu/openhtmltopdf/tree/open-dev-v1/openhtmltopdf-examples/src/main/resources/demos
		File pdfFile = File.createTempFile(pdfFilenName, ".pdf");
		try (OutputStream os = new FileOutputStream(pdfFile)) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			builder.useSVGDrawer(new BatikSVGDrawer());
			// Use the size define in the HTML
			// builder.useDefaultPageSize(150, 100, PageSizeUnits.MM);
			builder.withFile(htmlFile);
			builder.toStream(os);
			builder.run();
		}
		return pdfFile;
	}

	public static File createPDFFromHTML(String pdfFilenName, String htmlContent) throws Exception {
		// put the content in a temporary file
		File htmlFile = File.createTempFile(pdfFilenName, "html");

		// put content in file
		try (PrintWriter out = new PrintWriter(htmlFile, StandardCharsets.UTF_8.name())) {
			out.println(htmlContent);
		}

		return createPDFFromHTML(pdfFilenName, htmlFile);
	}
}
