from configparser import ConfigParser
import os
import cv2
import numpy as np

# Load pre-trained YOLOv3 model
MODEL_WEIGHTS = '/app/image_models/yolov3.weights'  # Path to YOLO model weights
MODEL_CONFIG = '/app/image_models/yolov3.cfg'  # Path to YOLO model configuration
CLASS_NAMES = '/app/image_models/yolov3.names'  # Path to YOLO model class names
#BASE_PATH = '/app/images/' # Base path for DMN analyzer

config = ConfigParser()
config.read('config.ini')
BASE_PATH = config.get('model', 'image_path')

def processImage(imagePath: str, conf_threshold: float = 0.5):
    if not imagePath or 'null' in imagePath:
        return None
    
    imagePath = BASE_PATH + imagePath

    if not os.path.isfile(imagePath):
        return None
    
    # Load the network architecture and weights
    net = cv2.dnn.readNetFromDarknet(MODEL_CONFIG, MODEL_WEIGHTS)

    # Load class names
    with open(CLASS_NAMES, 'r') as f:
        classes = [line.strip() for line in f.readlines()]

    # Load image
    image = cv2.imread(imagePath)

    # Prepare input image for YOLOv3
    blob = cv2.dnn.blobFromImage(image, 1/255.0, (416, 416), swapRB=True, crop=False)
    net.setInput(blob)

    # Forward pass through the network
    outs = net.forward(net.getUnconnectedOutLayersNames())

    # Parse output and extract detected objects
    #conf_threshold = 0.5  # Confidence threshold for object detection
    objects = []
    for out in outs:
        for detection in out:
            scores = detection[5:]
            class_id = np.argmax(scores)
            confidence = scores[class_id]
            if confidence > conf_threshold:
                objects.append(classes[class_id])

    objects = list(set(objects))
    return objects

#print(processImage(imagePath='dpn_img_qr_code_1682351691767.jpeg', conf_threshold=0.5))