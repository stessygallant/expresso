package com.sgitmanagement.expresso.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class ImageUtil {

	/**
	 * Will create a thumbnail image from the image file
	 * 
	 * @param imageFile
	 * @param thumbnailFile
	 * @return
	 * @throws Exception
	 */
	public static File createThumbnailImage(File imageFile, File thumbnailFile) throws Exception {
		int IMG_WIDTH = 70;
		int IMG_HEIGHT = 50;

		try (InputStream is = new FileInputStream(imageFile)) {
			resizeImage(is, thumbnailFile, IMG_WIDTH, IMG_HEIGHT);
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
	public static File resizeImage(File sourceImageFile, File targetImageFile, Integer maxWidth, Integer maxHeight) throws Exception {
		try (InputStream is = new FileInputStream(sourceImageFile)) {
			resizeImage(is, targetImageFile, maxWidth, maxHeight);
		}
		return targetImageFile;
	}

	/**
	 *
	 * @param input
	 * @param target
	 * @param width
	 * @param height
	 * @throws IOException
	 */
	private static void resizeImage(InputStream input, File targetFile, Integer maxWidth, Integer maxHeight) throws IOException {
		BufferedImage originalImage = ImageIO.read(input);

		// keep the ratio (do not stretch)
		int width = originalImage.getWidth();
		int height = originalImage.getHeight();

		if (maxWidth != null) {
			height = maxWidth * height / width;
			width = maxWidth;
		} else if (maxHeight != null) {
			width = maxHeight * width / height;
			height = maxHeight;
		}
		Image newResizedImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

		String fileName = targetFile.getName();
		String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
		ImageIO.write(convertToBufferedImage(newResizedImage), fileExtension, targetFile);
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
		int IMG_WIDTH = 70;
		int IMG_HEIGHT = 50;

		File thumbnailFile = getThumbnailImageFile(imageFile);
		ImageUtil.resizeImage(imageFile, thumbnailFile, IMG_WIDTH, IMG_HEIGHT);
		return thumbnailFile;
	}

	public static File getThumbnailImageFile(File imageFile) {
		String filePath = imageFile.getAbsolutePath();
		String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1);
		filePath = filePath.substring(0, filePath.lastIndexOf("."));
		File thumbnailFile = new File(filePath + "-thumbnail." + fileExtension);
		return thumbnailFile;
	}
}
