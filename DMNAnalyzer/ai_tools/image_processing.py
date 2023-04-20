import cv2
import numpy as np

# Load pre-trained YOLOv3 model
MODEL_WEIGHTS = 'image_models/yolov3.weights'  # Path to YOLO model weights
MODEL_CONFIG = 'image_models/yolov3.cfg'  # Path to YOLO model configuration
CLASS_NAMES = 'image_models/yolov3.names'  # Path to YOLO model class names

def processImage(imagePath: str):
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
    conf_threshold = 0.5  # Confidence threshold for object detection
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