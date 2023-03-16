package com.sgitmanagement.expresso.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

public class ImageUtil {
	public static final int THUMBNAIL_WIDTH = 70;
	public static final int THUMBNAIL_HEIGHT = 50;

	/**
	 * Will create a thumbnail image from the image file
	 * 
	 * @param imageFile
	 * @param thumbnailFile
	 * @return
	 * @throws Exception
	 */
	public static File createThumbnailImage(File imageFile, File thumbnailFile) throws Exception {
		try (InputStream is = new FileInputStream(imageFile)) {
			resizeImage(is, thumbnailFile, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
		}
		return thumbnailFile;
	}

	/**
	 * 
	 * @param sourceImageFile
	 * @param targetImageFile
	 * @param width
	 * @param height
	 * @return
	 * @throws Exception
	 */
	public static boolean resizeImage(File sourceImageFile, File targetImageFile, Integer maxWidth, Integer maxHeight) throws Exception {
		try (InputStream is = new FileInputStream(sourceImageFile)) {
			return resizeImage(is, targetImageFile, maxWidth, maxHeight);
		}
	}

	/**
	 * 
	 * @param inputStream
	 * @param targetFile
	 * @param maxWidth
	 * @param maxHeight
	 * @return true if resized
	 * @throws IOException
	 */
	private static boolean resizeImage(InputStream inputStream, File targetFile, Integer maxWidth, Integer maxHeight) throws IOException {
		BufferedImage originalImage = ImageIO.read(inputStream);

		// keep the ratio (do not stretch)
		int width = originalImage.getWidth();
		int height = originalImage.getHeight();

		boolean resize = false;
		if (maxWidth != null && width > maxWidth) {
			height = maxWidth * height / width;
			width = maxWidth;
			resize = true;
		} else if (maxHeight != null && height > maxHeight) {
			width = maxHeight * width / height;
			height = maxHeight;
			resize = true;
		}
		if (resize) {
			Image newResizedImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			String fileName = targetFile.getName();
			String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
			ImageIO.write(convertToBufferedImage(newResizedImage), fileExtension, targetFile);
		} else {
			FileUtils.copyInputStreamToFile(inputStream, targetFile);
		}
		return resize;
	}

	/**
	 *
	 * @param img
	 * @return
	 */
	private static BufferedImage convertToBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		// Create a buffered image without transparency
		BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = bi.createGraphics();
		graphics2D.drawImage(img, 0, 0, null);
		graphics2D.dispose();
		return bi;
	}

	/**
	 * Will create a thumbnail image from the image file
	 *
	 * @param imageFile
	 * @return the thumbnail image file
	 * @throws Exception
	 */
	public static File createThumbnailImage(File imageFile) throws Exception {
		File thumbnailFile = getImageFile(imageFile, "thumbnail");
		return createThumbnailImage(imageFile, thumbnailFile);
	}

	public static File getImageFile(File imageFile, String fileNameSuffix) {
		String filePath = imageFile.getAbsolutePath();
		String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1);
		filePath = filePath.substring(0, filePath.lastIndexOf("."));
		File thumbnailFile = new File(filePath + "-" + fileNameSuffix + "." + fileExtension);
		return thumbnailFile;
	}
}
