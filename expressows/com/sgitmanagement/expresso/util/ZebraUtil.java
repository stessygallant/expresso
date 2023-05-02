package com.sgitmanagement.expresso.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterResolution;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.util.XRLog;
import com.sgitmanagement.expresso.exception.ValidationException;

import uk.org.okapibarcode.backend.QrCode;
import uk.org.okapibarcode.output.SvgRenderer;

public enum ZebraUtil {
	INSTANCE;

	final private Logger logger = LoggerFactory.getLogger(ZebraUtil.class);

	static {
		XRLog.setLoggingEnabled(false);
		// XRLog.setLoggerImpl(new Slf4jLogger());
	}

	public enum Type {
		// small(55, 19), // 2.25" X 0.75"
		small(76, 25), // 3"X1"
		large(102, 152), // 4"X6"
		custom();

		private int width;
		private int height;

		private Type() {

		}

		private Type(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public boolean isRotated(int width, int height) {
			return (this.width > this.height) ? height > width : width > height;
		}
	};

	/**
	 * 
	 * @param printerName
	 * @return
	 * @throws Exception
	 */
	private PrintService getPrintService(String printerName) throws Exception {
		if (printerName == null) {
			printerName = SystemEnv.INSTANCE.getDefaultProperties().getProperty("default_printer");
		}

		if (printerName != null) {
			// printerName = printerName.toLowerCase();
			logger.debug("Looking for [" + printerName + "]");
			for (PrintService printService : PrinterJob.lookupPrintServices()) {
				if (printService.getName().equals(printerName)) {
					return printService;
				}
			}
		}
		logger.warn("Cannot find printer [" + printerName + "]");
		Map<String, Object> params = new HashMap<>();
		params.put("printerName", printerName);
		throw new ValidationException("unableToLocatePrinter", params);
	}

	public File createQRCode(String content) throws Exception {
		QrCode barcode = new QrCode();
		barcode.setContent(content);

		File qrBarCodeFile = File.createTempFile("qrCode", ".svg");
		OutputStream os = new FileOutputStream(qrBarCodeFile);
		double magnification = 1.0;
		SvgRenderer renderer = new SvgRenderer(os, magnification, Color.WHITE, Color.BLACK, false);
		renderer.render(barcode);
		os.close();

		return qrBarCodeFile;
	}

	/**
	 *
	 * @param zebraTemplate
	 * @param type
	 * @throws Exception
	 */
	public void printSticker(String resourceName, String zebraTemplateContent, String printerName, Type type, int quantity, Map<String, String> params) throws Exception {
		File pdfFile = null;
		File pngFile = null;

		try {
			pdfFile = createPDFSticker(resourceName, zebraTemplateContent, type, params);
			boolean rotated = false;

			// Convert PDF to PNG
			int dpi = 203;// Zebra ZD420 DPI
			pngFile = File.createTempFile(resourceName, ".png");
			try (PDDocument document = PDDocument.load(pdfFile, MemoryUsageSetting.setupTempFileOnly())) {
				PDFRenderer pdfRenderer = new PDFRenderer(document);
				// only first page
				int page = 0;
				// pixels (1px = 1/96th of 1in)
				// inches (1in = 96px = 2.54cm)
				// RGB is nicer for text but BINARY is better for QR code
				// GRAY is a compromise
				BufferedImage bim = pdfRenderer.renderImageWithDPI(page, dpi, ImageType.GRAY);
				rotated = type.isRotated(bim.getWidth(), bim.getHeight());
				ImageIOUtil.writeImage(bim, pngFile.getAbsolutePath(), dpi);
			}

			boolean usePrintService = false;
			if (usePrintService) {
				// print a label on the zebra printer
				PrintService printService = getPrintService(printerName);
				DocPrintJob job = printService.createPrintJob();

				try (FileInputStream fileInputStream = new FileInputStream(pngFile)) {
					DocAttributeSet das = new HashDocAttributeSet();
					das.add(new PrinterResolution(dpi, dpi, PrinterResolution.DPI));
					das.add(new MediaPrintableArea(0, 0, type.getWidth(), type.getHeight(), MediaPrintableArea.MM));
					Doc doc = new SimpleDoc(fileInputStream, DocFlavor.INPUT_STREAM.PNG, das);

					PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
					pras.add(new Copies(quantity));
					pras.add(Chromaticity.MONOCHROME);
					pras.add(PrintQuality.HIGH);

					if (rotated) {
						pras.add(OrientationRequested.LANDSCAPE);
					}
					job.print(doc, pras);
				}
			} else {
				// use LP directly
				// -c Exit only after further access to any of the input files is no longer required. The application can then safely delete or modify the files without affecting the output operation.
				// -n quantity

				// PATCH: when printing all in a batch, the latest are poor quality (problem with the ink or the paper)
				// wait between each printing
				boolean bacthPrinting = false;
				if (bacthPrinting) {
					Process p = Runtime.getRuntime().exec("lp -c -n " + quantity + " -d " + printerName + " " + pngFile.getAbsolutePath());
					p.waitFor();
				} else {
					for (int i = 0; i < quantity; i++) {
						Process p = Runtime.getRuntime().exec("lp -c -d " + printerName + " " + pngFile.getAbsolutePath());
						p.waitFor();
						Thread.sleep(1000); // sleep between each print
					}
				}
				pngFile.delete();
			}
		} finally {

			// then delete the files
			if (pdfFile != null) {
				pdfFile.delete();
			}
			if (pngFile != null) {
				// wait before
				// Thread.sleep(2000);
				// pngFile.delete();
			}
		}
	}

	/**
	 *
	 * @param zebraTemplate
	 * @param type
	 * @throws Exception
	 */
	public File createPDFSticker(String resourceName, String zebraTemplateContent, Type type, Map<String, String> params) throws Exception {
		File htmlfFile = null;
		File pdfFile = null;

		try {
			String htmlContent = Util.replacePlaceHolders(zebraTemplateContent, params, true);

			htmlfFile = File.createTempFile(resourceName, ".html");

			// write in UTF-8
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(htmlfFile), StandardCharsets.UTF_8));
			writer.write(htmlContent);
			writer.close();

			// Create PDF from HTML
			// https://gitee.com/chqiuu/openhtmltopdf
			// https://gitee.com/chqiuu/openhtmltopdf/tree/open-dev-v1/openhtmltopdf-examples/src/main/resources/demos
			pdfFile = File.createTempFile(resourceName, ".pdf");
			try (OutputStream os = new FileOutputStream(pdfFile)) {
				PdfRendererBuilder builder = new PdfRendererBuilder();
				builder.useFastMode();
				builder.useSVGDrawer(new BatikSVGDrawer());
				// Use the size define in the HTML but this one if none is set
				// builder.useDefaultPageSize(type.getWidth(), type.getHeight(), PageSizeUnits.MM);
				builder.withFile(htmlfFile);
				builder.toStream(os);
				builder.run();
			}
		} finally {

			// then delete the files
			if (htmlfFile != null) {
				htmlfFile.delete();
			}
		}
		return pdfFile;
	}
}
