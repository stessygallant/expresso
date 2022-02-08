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
import java.net.InetAddress;
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
import javax.print.attribute.standard.PrinterResolution;
import javax.servlet.http.HttpServletRequest;

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
		// Printer must be configured:
		// Options: mm, portrait, w=58, H=18, rotate 180
		// Advanced:Left position: -23, Top:2
		small(58, 18, 180, false, -23, 2),

		// Printer must be configured:
		medium(70, 30, 0, false, 0, 0),

		// Printer must be configured:
		// Options: mm, Landscape, w=100, H=150
		large(100, 150, 0, true, 0, 0),

		// user defined
		custom();

		private int width;
		private int height;
		private int rotate;
		private boolean landscape;
		private int offsetLeft;
		private int offsetTop;

		private Type(int width, int height, int rotate, boolean landscape, int offsetLeft, int offsetTop) {
			this.width = width;
			this.height = height;
			this.rotate = rotate;
			this.landscape = landscape;
			this.offsetLeft = offsetLeft;
			this.offsetTop = offsetTop;
		}

		private Type(int width, int height, int rotate, boolean landscape) {
			this(width, height, rotate, landscape, 0, 0);
		}

		private Type() {

		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public int getRotate() {
			return rotate;
		}

		public boolean isLandscape() {
			return landscape;
		}

		public int getOffsetLeft() {
			return offsetLeft;
		}

		public int getOffsetTop() {
			return offsetTop;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public void setRotate(int rotate) {
			this.rotate = rotate;
		}

		public void setLandscape(boolean landscape) {
			this.landscape = landscape;
		}

		public void setOffsetLeft(int offsetLeft) {
			this.offsetLeft = offsetLeft;
		}

		public void setOffsetTop(int offsetTop) {
			this.offsetTop = offsetTop;
		}

	};

	/**
	 * Return the print service for the Zebra. It takes the IP address of the request and find the computer name
	 *
	 * @param request
	 * @param type
	 * @return
	 * @throws Exception
	 */
	private PrintService getPrintService(HttpServletRequest request, Type type) throws Exception {
		if (request != null) {
			String ip = Util.getIpAddress(request);
			InetAddress addr = InetAddress.getByName(ip);
			String hostName = addr.getHostName();
			if (hostName != null && !hostName.equals(ip)) {
				if (hostName.contains(".")) {
					hostName = hostName.substring(0, hostName.indexOf('.'));
				}
				logger.debug("Request from [" + ip + "]:[" + hostName + "]");

				String printerName = "zebra-" + hostName + "-" + type.name();
				printerName = printerName.toLowerCase();
				logger.debug("Looking for [" + printerName + "]");
				for (PrintService printService : PrinterJob.lookupPrintServices()) {
					if (printService.getName().equals(printerName)) {
						return printService;
					}
				}
				logger.warn("Cannot find printer [" + printerName + "]");
				Map<String, Object> params = new HashMap<>();
				params.put("printerName", printerName);
				throw new ValidationException("unableToLocatePrinter", params);
			} else {
				logger.warn("Cannot find hostname for IP [" + ip + "]");
				Map<String, Object> params = new HashMap<>();
				params.put("ip", ip);
				throw new ValidationException("unableToResolveIP", params);
			}
		} else {
			// for testing purpose
			String printerName = SystemEnv.INSTANCE.getDefaultProperties().getProperty("default_printer");
			for (PrintService printService : PrinterJob.lookupPrintServices()) {
				if (printService.getName().equals(printerName)) {
					return printService;
				}
			}
			return null;
		}
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
	public void printSticker(String resourceName, String zebraTemplateContent, Type type, Map<String, String> params, HttpServletRequest request) throws Exception {
		File pdfFile = null;
		File pngfFile = null;

		try {
			pdfFile = createPDFSticker(resourceName, zebraTemplateContent, type, params);

			// Convert PDF to PNG
			int dpi = 203;// Zebra ZD420 DPI
			pngfFile = File.createTempFile(resourceName, ".png");
			try (PDDocument document = PDDocument.load(pdfFile)) {
				PDFRenderer pdfRenderer = new PDFRenderer(document);
				// only first page
				int page = 0;
				// pixels (1px = 1/96th of 1in)
				// inches (1in = 96px = 2.54cm)
				// RGD is nicer for text but BINARY is better for QR code
				// GRAY is a compromise
				BufferedImage bim = pdfRenderer.renderImageWithDPI(page, dpi, ImageType.GRAY);
				ImageIOUtil.writeImage(bim, pngfFile.getAbsolutePath(), dpi);
				document.close();
			}

			// print a label on the zebra printer
			PrintService printService = getPrintService(request, type);
			if (printService != null) {

				DocPrintJob job = printService.createPrintJob();

				DocAttributeSet das = new HashDocAttributeSet();
				das.add(new PrinterResolution(dpi, dpi, PrinterResolution.DPI));
				FileInputStream fileInputStream = new FileInputStream(pngfFile);
				Doc doc = new SimpleDoc(fileInputStream, DocFlavor.INPUT_STREAM.PNG, das);

				PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
				pras.add(new Copies(1));
				pras.add(new PrinterResolution(dpi, dpi, PrinterResolution.DPI));
				pras.add(Chromaticity.MONOCHROME);
				pras.add(new MediaPrintableArea(0, 0, type.getWidth(), type.getHeight(), MediaPrintableArea.MM));

				job.print(doc, pras);
				fileInputStream.close();
			}
		} finally {

			// then delete the files
			if (SystemEnv.INSTANCE.isInProduction()) {
				if (pdfFile != null) {
					pdfFile.delete();
				}
				if (pngfFile != null) {
					// pngfFile.delete();
				}
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
				// Use the size define in the HTML
				// builder.useDefaultPageSize(150, 100, PageSizeUnits.MM);
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
