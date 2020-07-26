from flask import Flask, request
import cv2 as cv
import numpy as np
import json
import base64


# Load YOLO
net = cv.dnn.readNet("yolov3.weights","yolov3.cfg")
classes = []
with open("coco.names", "r") as f: classes = [line.strip() for line in f.readlines()]
print("recognizer# Yolo3 loaded with %d classes" % len(classes))

layer_names = net.getLayerNames()
outputlayers = [layer_names[i[0] - 1] for i in net.getUnconnectedOutLayers()]

app = Flask(__name__)

config_debug = False


def detect(img):
    height, width, channels = img.shape

    blob = cv.dnn.blobFromImage(img, 0.00392, (416, 416), (0, 0, 0), True, crop=False)
    net.setInput(blob)
    outs = net.forward(outputlayers)

    # Showing info on screen/ get confidence score of algorithm in detecting an object in blob
    class_ids = []
    labels = []
    confidences = []
    boxes = []
    for out in outs:
        for detection in out:
            scores = detection[5:]
            class_id = np.argmax(scores)
            confidence = scores[class_id]
            if confidence > 0.5:
                # onject detected
                center_x = int(detection[0] * width)
                center_y = int(detection[1] * height)
                w = int(detection[2] * width)
                h = int(detection[3] * height)

                # cv2.circle(img,(center_x,center_y),10,(0,255,0),2)
                # rectangle co-ordinaters
                x = int(center_x - w / 2)
                y = int(center_y - h / 2)
                # cv2.rectangle(img,(x,y),(x+w,y+h),(0,255,0),2)

                boxes.append([x, y, w, h])  # put all rectangle areas
                confidences.append(float(confidence))  # how confidence was that object detected and show that percentage
                class_ids.append(int(class_id))  # name of the object tha was detected
                labels.append(str(classes[int(class_id)]))
    return {
        "classIds" : class_ids,
        "labels" : labels,
        "confidences" : confidences,
        "boxes" : boxes
    }


@app.route("/get_metadata")
def get_metadata():
    return json.dumps({
        "consumed_data" : {
            "camera_frame": {
                "payloadType" : "com.unigent.agentbase.library.core.state.ImagePayload"
            }
        },
        "produced_data" : {
            "detected_objects": {
                "payloadType" : "com.unigent.agentbase.library.core.state.JsonPayload"
            },
            "detected_objects_debug": {
                "payloadType": "com.unigent.agentbase.library.core.state.ImagePayload"
            }
        },
        "config_data" : {
            "debug" : True
        },
        "description" : "YOLO v3 to detect objects by 416x416 image"
    })


@app.route("/configure", methods=['POST'])
def configure():
    r = request
    cfg = r.json

    global config_debug
    config_debug = cfg["debug"] == "true"
    print("recognizer# Configured! Debug=%s" % config_debug)

    return "OK"


@app.route("/on_state_update/<local_binding>/<origin>/<timestamp>/<correlation_id>", methods=['POST'])
def on_situation(local_binding, origin, timestamp, correlation_id):

    global config_debug

    if local_binding == "camera_frame":
        r = request
        nparr = np.fromstring(r.data, np.uint8)
        img = cv.imdecode(nparr, cv.IMREAD_COLOR)
        if img is None:
            raise Exception("Unable to decode provided image")

        detection_result = detect(img)
        print(detection_result)

        produced_data = [{
            "localBinding": "detected_objects",
            "correlationId": correlation_id,
            "data": detection_result
        }]

        if config_debug:
            boxes = detection_result["boxes"]
            for i in range(len(boxes)):
                box = boxes[i]
                cv.rectangle(img, (box[0], box[1]), (box[0] + box[2], box[1] + box[3]), (100, 100, 255), 2)
            debug_image = cv.resize(img, (200, 200), interpolation = cv.INTER_AREA)

            # Convert image to PNG and Base64 encode it for JSON transport
            is_success, debug_image_png = cv.imencode(".png", debug_image)
            debug_image_png_bytes = debug_image_png.tobytes()
            debug_image_png_bytes_b64 = base64.b64encode(debug_image_png_bytes).decode('utf-8')

            produced_data.append({
                "localBinding": "detected_objects_debug",
                "correlationId": correlation_id,
                "data": debug_image_png_bytes_b64
            })

        return json.dumps({
            "producedData": produced_data
        })
    else:
        return "{}"


app.run(debug=True, host="192.168.1.2", port=5050)