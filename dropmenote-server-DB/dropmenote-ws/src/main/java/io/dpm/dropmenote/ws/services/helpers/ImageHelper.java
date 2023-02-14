package io.dpm.dropmenote.ws.services.helpers;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dpm.dropmenote.ws.enums.ImageTypeEnum;
import io.dpm.dropmenote.ws.utils.EnumUtil;
import io.dpm.dropmenote.ws.utils.ImageUtil;

/**
 * copied from Doddo and changed
 * @author Peter
 *
 */
public class ImageHelper {

    private static Logger LOG = LoggerFactory.getLogger(ImageHelper.class);

	private static final List<String> IMAGE_TYPES = EnumUtil.contenet(ImageTypeEnum.BASE64_BMP_PREFIX, ImageTypeEnum.BASE64_JPEG_PREFIX,
			ImageTypeEnum.BASE64_JPG_PREFIX, ImageTypeEnum.BASE64_PNG_PREFIX); 
    
    private static final String JPEG = ".jpeg";
    private static final String IMG_PREFIX = "dpn_img_";

    /**
     * 
     * @param imageString - base64
     * @param aditionalUuid
     * @param imageFilePath
     * @param lastImageName
     * @return
     */
    public static String saveImage(String imageString, String aditionalUuid, String imageFilePath, String lastImageName) {
        deleteFoto(lastImageName, imageFilePath);
        String fileName;
        if (imageString != null) {
            if (imageString.contains(ImageTypeEnum.BASE64_JPEG_PREFIX.getContenet())) {
                imageString = imageString.replaceFirst(ImageTypeEnum.BASE64_JPEG_PREFIX.getContenet(), "");
            } else if (imageString.contains(ImageTypeEnum.BASE64_JPG_PREFIX.getContenet())) {
            	imageString = imageString.replaceFirst(ImageTypeEnum.BASE64_JPG_PREFIX.getContenet(), "");
			}  else if (imageString.contains(ImageTypeEnum.BASE64_PNG_PREFIX.getContenet())) {
            	imageString = imageString.replaceFirst(ImageTypeEnum.BASE64_PNG_PREFIX.getContenet(), "");
			}  else if (imageString.contains(ImageTypeEnum.BASE64_BMP_PREFIX.getContenet())) {
            	imageString = imageString.replaceFirst(ImageTypeEnum.BASE64_BMP_PREFIX.getContenet(), "");
			}

            long now = new Date().getTime();

            if (aditionalUuid != null) {
                fileName = IMG_PREFIX + aditionalUuid + "_" + now + JPEG;
            } else {
                fileName = IMG_PREFIX + now + JPEG;
            }
            LOG.debug("save image {} , {}", imageString, imageFilePath + fileName);
            ImageUtil.decodeBase64ResizeAndSaveToFile(imageString, imageFilePath + fileName);
        } else {
            LOG.warn("Image not found");
            return null;
        }
        return fileName;
    }

    /**
     * delete file
     * @param lastImageName
     * @param imageFilePath
     */
    public static void deleteFoto(String lastImageName, String imageFilePath) {
        if (lastImageName != null) {
            String prevImagePath = imageFilePath + lastImageName;
            File fileToDelete = new File(prevImagePath);
            if (fileToDelete.delete()) {
                LOG.debug("Previous photo was deleted");
            } else {
                LOG.warn("Previous photo was not deleted");
            }
        }
    }

}
