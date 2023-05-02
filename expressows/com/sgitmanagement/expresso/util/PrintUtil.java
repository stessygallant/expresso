package com.sgitmanagement.expresso.util;

import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.Sides;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

/**
 * Provide functions for printing:<br>
 * - Print PDF file <br>
 * - Monitor print job
 */
public class PrintUtil {
	/**
	 * Return the list of printer names available
	 * 
	 * @return
	 */
	public static Set<String> getPrinterList() {
		Set<String> list = new HashSet<String>();
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
		for (PrintService printService : printServices) {
			list.add(printService.getName());
		}
		return list;
	}

	/**
	 * Get the print service for a specific printer name
	 * 
	 * @param printerName
	 * @return
	 * @throws PrinterException
	 */
	public static PrintService getPrintService(String printerName) throws PrinterException {
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
		for (PrintService printService : printServices) {
			if (printService.getName().equalsIgnoreCase(printerName)) {
				return printService;
			}
		}

		throw new PrinterException("Cannot find printer [" + printerName + "]");
	}

	public static void printPdf(String printerName, File pdfFile) throws Exception {
		printPdf(printerName, pdfFile, null);
	}

	public static void printPdf(String printerName, File pdfFile, PrintUserListener listener) throws Exception {
		printPdf(printerName, pdfFile, listener, true, false, false);
	}

	public static void printPdf(String printerName, File pdfFile, PrintUserListener listener, boolean blackAndWhite, boolean duplex, boolean landscape) throws Exception {
		FileInputStream pdfInputStream = new FileInputStream(pdfFile);
		printPdf(printerName, pdfInputStream, listener, blackAndWhite, duplex, landscape);
		pdfInputStream.close();
	}

	/**
	 * Print PDF file with current document page configuration
	 * 
	 * @param printerName   Printer name
	 * @param pdfFile       PDF file to print
	 * @param listener      Listeners for fail/completed
	 * @param duplex
	 * @param blackAndWhite
	 * @throws Exception
	 */
	public static void printPdf(String printerName, InputStream pdfInputStream, PrintUserListener listener, boolean blackAndWhite, boolean duplex, boolean landscape) throws Exception {
		final String fPrinterName = (SystemEnv.INSTANCE.isInProduction() ? printerName : SystemEnv.INSTANCE.getDefaultProperties().getProperty("default_printer"));

		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			executor.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {

					PrintService printService = getPrintService(fPrinterName);

					// Create the document to be printed
					try (PDDocument document = PDDocument.load(pdfInputStream, MemoryUsageSetting.setupTempFileOnly())) {
						Doc doc = new SimpleDoc(new PDFPrintable(document, Scaling.SHRINK_TO_FIT), DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);

						// Print it
						PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
						// pras.add(MediaSizeName.NA_LETTER);
						pras.add(duplex ? Sides.DUPLEX : Sides.ONE_SIDED);
						pras.add(MediaSizeName.NA_LETTER);
						pras.add(blackAndWhite ? Chromaticity.MONOCHROME : Chromaticity.COLOR);
						pras.add(PrintQuality.HIGH);
						pras.add(landscape ? OrientationRequested.LANDSCAPE : OrientationRequested.PORTRAIT);
						DocPrintJob pjob = printService.createPrintJob();
						PrintJobWatcher pjDone = new PrintJobWatcher(pjob);

						pjob.print(doc, pras);

						if (listener != null) {

							// Wait for the print job to be done
							pjDone.waitForDone();

							// the printing is completed
							if (pjDone.isSucessful()) {
								listener.jobPrinted();
							} else {
								listener.jobFailed(new Exception("" + pjDone.getPrintJobEvent()));
							}
						}
						return null;
					}
				}
			}).get(2, TimeUnit.MINUTES);
		} finally {
			executor.shutdown();
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println(getPrinterList());

		final String printerName = "\\\\CAVALSRV3003VP.ANYACCESS.NET\\CAVALMFD095";

		final File pdfFile = new File("C:\\Users\\sgallant-c\\Desktop\\a.pdf");
		System.out.println("Calling " + new Date());

		final PrintUserListener listener = new PrintUserListener() {
			@Override
			public void jobPrinted() {
				System.out.println("Printed " + new Date());
			}

			@Override
			public void jobFailed(Exception e) {
				System.out.println("Failed " + new Date() + ": " + e);
				e.printStackTrace();
			}
		};

		new Thread() {
			@Override
			public void run() {
				try {
					PrintUtil.printPdf(printerName, pdfFile, listener);
				} catch (Exception e) {
					listener.jobFailed(e);
				}
			}
		}.start();
		System.out.println("Completed " + new Date());
	}

	/**
	 * Interface implemented by the client to be notified when a Prinf Job is terminated
	 * 
	 */
	static public interface PrintUserListener {
		public void jobPrinted();

		public void jobFailed(Exception e);
	}

}

/**
 * Class that it used to monitor the print job
 */
class PrintJobWatcher {
	// true if it is safe to close the print job's input stream
	private boolean done = false;
	private boolean success;
	private PrintJobEvent pje;

	PrintJobWatcher(DocPrintJob job) {

		// Add a listener to the print job
		job.addPrintJobListener(new PrintJobAdapter() {
			@Override
			public void printJobCanceled(PrintJobEvent pje) {
				finish(false, pje);
			}

			@Override
			public void printJobCompleted(PrintJobEvent pje) {
				finish(true, pje);
			}

			@Override
			public void printJobFailed(PrintJobEvent pje) {
				finish(false, pje);
			}

			@Override
			public void printJobNoMoreEvents(PrintJobEvent pje) {
				// this is bad. It means that the printer is not able to send
				// event when the job is completed
				// we have to assume that it works
				// System.out.println("Printer has limited capabilities");
				finish(true, pje);
			}

			void finish(boolean success, PrintJobEvent pje) {
				synchronized (PrintJobWatcher.this) {
					PrintJobWatcher.this.success = success;
					PrintJobWatcher.this.done = true;
					PrintJobWatcher.this.pje = pje;
					PrintJobWatcher.this.notify();
				}
			}
		});
	}

	public boolean isSucessful() {
		return success;
	}

	public PrintJobEvent getPrintJobEvent() {
		return pje;
	}

	public synchronized void waitForDone() {
		try {
			while (!done) {
				wait();
			}
		} catch (InterruptedException e) {
		}
	}
}
