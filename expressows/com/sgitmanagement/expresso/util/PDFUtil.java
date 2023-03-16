package com.sgitmanagement.expresso.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.PageSizeUnits;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;

/**
 * 
 */
public class PDFUtil {
	public static File createPDFFromHTML(String pdfFileName, File htmlFile, Integer pageWidthMM, Integer pageHeightMM) throws Exception {
		// Create PDF from HTML
		// https://gitee.com/chqiuu/openhtmltopdf
		// https://gitee.com/chqiuu/openhtmltopdf/tree/open-dev-v1/openhtmltopdf-examples/src/main/resources/demos
		File pdfFile = File.createTempFile(pdfFileName, ".pdf");
		try (OutputStream os = new FileOutputStream(pdfFile)) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			builder.useSVGDrawer(new BatikSVGDrawer());

			if (pageWidthMM != null && pageHeightMM != null) {
				builder.useDefaultPageSize(pageWidthMM, pageHeightMM, PageSizeUnits.MM);
			}

			builder.withFile(htmlFile);
			builder.toStream(os);
			builder.run();
		}
		return pdfFile;
	}

	public static File createPDFFromHTML(String pdfFileName, File htmlFile) throws Exception {
		return createPDFFromHTML(pdfFileName, htmlFile, null, null);
	}

	public static File createPDFFromHTML(String pdfFileName, String htmlContent) throws Exception {
		return createPDFFromHTML(pdfFileName, htmlContent, null, null);
	}

	public static File createPDFFromHTML(String pdfFileName, String htmlContent, Integer pageWidthMM, Integer pageHeightMM) throws Exception {
		// put the content in a temporary file
		File htmlFile = File.createTempFile(pdfFileName, "html");

		// put content in file
		try (PrintWriter out = new PrintWriter(htmlFile, StandardCharsets.UTF_8.name())) {
			out.println(htmlContent);
		}

		return createPDFFromHTML(pdfFileName, htmlFile, pageWidthMM, pageHeightMM);
	}
}
