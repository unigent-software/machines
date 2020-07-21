from flask import Flask, request
import cv2 as cv
import numpy as np
import json

# Load YOLO
net = cv.dnn.readNet("yolov3.weights","yolov3.cfg")
classes = []
with open("coco.names", "r") as f: classes = [line.strip() for line in f.readlines()]
print("recognizer# Yolo3 loaded with %d classes" % len(classes))

layer_names = net.getLayerNames()
outputlayers = [layer_names[i[0] - 1] for i in net.getUnconnectedOutLayers()]

app = Flask(__name__)


def detect(img, correlationid):
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
        "producedData": [
            {
                "localBinding" : "detected_objects",
                "correlationId" : correlationid,
                "data" : {
                    "classIds" : class_ids,
                    "labels" : labels,
                    "confidences" : confidences,
                    "boxes" : boxes
                }
            }
        ]
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
            }
        },
        "config_data" : {
        }
    })


@app.route("/on_state_update/<local_binding>/<origin>/<timestamp>/<correlationid>", methods=['POST'])
def on_situation(local_binding, origin, timestamp, correlationid):
    if local_binding == "camera_frame":
        r = request
        nparr = np.fromstring(r.data, np.uint8)
        # decode image
        img = cv.imdecode(nparr, cv.IMREAD_COLOR)
        detection_result = detect(img, correlationid)
        print(detection_result)
        return json.dumps(detection_result)
    else:
        return "{}"


app.run(debug=True, host="192.168.1.2", port=5050)