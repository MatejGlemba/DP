package io.dpm.dropmenote.ws.utils;

import io.dpm.dropmenote.ws.services.helpers.ImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

/**
 * copied from Doddo
 * 
 * @author Peter
 *
 */
public class ImageUtil {
	private static Logger LOG = LoggerFactory.getLogger(ImageUtil.class);
	private static int MAX_WIDTH_DEFAULT = 800;
	private static String IMAGE_FILE_TYPE = "JPEG";

	/**
	 * 
	 * @param imageAsByteArray
	 * @param width
	 * @param height
	 * @return
	 * @throws Exception
	 */
	public static byte[] scale(byte[] imageAsByteArray, int width, int height) throws Exception {
		ByteArrayInputStream in = new ByteArrayInputStream(imageAsByteArray);
		try {
			BufferedImage img = ImageIO.read(in);
			if (height == 0) {
				height = (width * img.getHeight()) / img.getWidth();
			}
			if (width == 0) {
				width = (height * img.getWidth()) / img.getHeight();
			}
			Image scaledImage = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			BufferedImage imageBuff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			imageBuff.getGraphics().drawImage(scaledImage, 0, 0, new Color(0, 0, 0), null);

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			ImageIO.write(imageBuff, IMAGE_FILE_TYPE, buffer);

			return buffer.toByteArray();
		} catch (IOException e) {
			throw new Exception("IOException in scale. Reason:" + LogMessageUtil.getExceptionMessage(e));
		}
	}

	/**
	 * Calculate new hegiht for the image. Keep aspect ration from the original
	 * image
	 * 
	 * @param originalWidth
	 * @param originalHeight
	 * @param newWidth
	 * @return
	 */
	public static int calculateHeightByAspectRatio(double originalWidth, double originalHeight, double newWidth) {
		return (int) (originalHeight / originalWidth * newWidth);
	}

	public static int calculateWidthByAspectRatio(double originalHeight, double originalWidth, double newHeight) {
		return (int) (originalWidth / originalHeight * newHeight);
	}

	public static int calculateHeightByAspectRatio(BufferedImage bufferedImage) {
		double originalWidthDouble = bufferedImage.getWidth();
		double originalHeightDouble = bufferedImage.getHeight();
		double newHeightDouble = MAX_WIDTH_DEFAULT;
		return (int) (originalHeightDouble / originalWidthDouble * newHeightDouble);
	}

	public static void decodeBase64SaveToFile(String base64, String fileName) {
		File outputFile = new File(fileName);
		try {
			outputFile.getParentFile().mkdirs();
			BufferedImage image = base64ToImg(base64);
			if (image != null) {
				// ImageIO.write(image, IMAGE_FILE_TYPE, outputFile);
				saveAsJpeg(image, outputFile, 0.6f);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * resize by MAX_WIDTH_DEFAULT (800) (if imagesize is bigger). save as jpeg
	 * 
	 * @param base64
	 * @param fileName
	 */
	public static void decodeBase64ResizeAndSaveToFile(String base64, String fileName) {
		File outputFile = new File(fileName);
		try {
			outputFile.getParentFile().mkdirs();
			BufferedImage image = base64ToImg(base64);

			if (image.getWidth() > MAX_WIDTH_DEFAULT) {
				image = resize(image);
			}
			if (image != null) {
				LOG.info("SAVE IMAGE: image {}, outputFile {}", image, outputFile.getAbsolutePath());
				Path currentRelativePath = Paths.get("");
				String s = currentRelativePath.toAbsolutePath().toString();
				LOG.info("current path: {}", s);
				saveAsJpeg(image, outputFile, 0.6f);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * resize custom if only one size on imput - second will be in image aspect
	 * ratio
	 * 
	 * @param base64
	 * @param fileName
	 * @param newWidth
	 * @param newHeight
	 */
	public static void decodeBase64ResizeAndSaveToFile(String base64, String fileName, Integer newWidth, Integer newHeight) {
		File outputFile = new File(fileName);
		try {
			outputFile.getParentFile().mkdirs();
			BufferedImage image = base64ToImg(base64);

			if (newWidth == null && newHeight == null) {
				image = resize(image);
			} else {
				resize(image, newWidth, newHeight);
			}
			if (image != null) {
				// ImageIO.write(image, IMAGE_FILE_TYPE, outputFile);
				saveAsJpeg(image, outputFile, 0.6f);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * to image without resize
	 * 
	 * @param base64
	 * @return
	 */
	private static BufferedImage base64ToImg(String base64) {
		BufferedImage image = null;
		byte[] imageByte = null;

		imageByte = Base64.getDecoder().decode((base64.getBytes()));
		ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
		try {
			image = ImageIO.read(bis);
			bis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return image;
	}

	/**
	 * 
	 * @param img
	 * @return
	 */
	private static BufferedImage resize(BufferedImage img) {
		int newH = calculateHeightByAspectRatio(img);
		Image tmp = img.getScaledInstance(MAX_WIDTH_DEFAULT, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(MAX_WIDTH_DEFAULT, newH, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return dimg;
	}

	/**
	 * 
	 * @param img
	 * @param newW
	 * @param newH
	 * @return
	 */
	private static BufferedImage resize(BufferedImage img, Integer newW, Integer newH) {
		if (newH == null) {
			newH = calculateHeightByAspectRatio(img.getWidth(), img.getHeight(), newW);
		}
		if (newW == null) {
			newW = calculateWidthByAspectRatio(img.getHeight(), img.getWidth(), newH);
		}
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return dimg;
	}

	/**
	 * 
	 * @param image
	 * @param outputFile
	 * @param quality
	 * @throws IOException
	 */
	public static void saveAsJpeg(BufferedImage image, File outputFile, float quality) throws IOException {
		BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = result.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();

		ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(0.7f);

		jpgWriter.setOutput(ImageIO.createImageOutputStream(outputFile));
		IIOImage outputImage = new IIOImage(result, null, null);
		jpgWriter.write(null, outputImage, jpgWriteParam);
		jpgWriter.dispose();
	}
}