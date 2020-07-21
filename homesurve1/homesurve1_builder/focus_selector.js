JsonPayload = Java.type("com.unigent.agentbase.library.core.state.JsonPayload");

agentbase_processor_metadata = {
    "description" : "Selects focus from the scene"
}

// Define types of data this processor consumes so builder can bind the inputs it properly
consumed_data = {
    "detected_objects" : {
    		payloadType: "ab_core_state:JsonPayload"
    }
}

// Define types of data this processor produces so builder can bind the outputs it properly
produced_data = { 
    "object_in_focus" : {
    		payloadType: "ab_core_state:JsonPayload"
    }
}

var currentFocus = null;

function on_state_update(state_update, source_topic, local_binding, env) {
	env.log.debug("Received " + local_binding + ": " + state_update.payload)
	if (local_binding === "detected_objects") {
		// {'classIds': [], 'boxes': [], 'confidences': [], 'labels': []}
		var detectedObjects = eval("(" + state_update.payload.json + ")")

		if(detectedObjects.classIds.length == 0) {
			if(currentFocus) {
				env.stateBus.publish("object_in_focus", null);
				currentFocus = null;
				env.log.debug("Lost focus (no objects)")
			}			
		}
		else {
			env.log.debug("Detected objects: " + state_update.payload);
			if(currentFocus) {
				// Look for focus in the detected objects
				var found = false;
				for(var i=0; i<detectedObjects.classIds.length; i++) {
					if(detectedObjects.classIds[i] === currentFocus.classId) {
						// Found!
						currentFocus = {
							classId: detectedObjects.classIds[i],
							label: detectedObjects.labels[i],
							box: detectedObjects.boxes[i]
						}
						env.log.debug("Found focus")
						found = true;
						break;
					}
				}

				if(!found) {
					env.log.debug("Lost focus (not found)")
				}
			}


			if(!currentFocus) {
				pickNewFocus(detectedObjects);
				env.log.debug("Picked new focus " + currentFocus)
			}			
		}

		env.stateBus.publish("object_in_focus", JsonPayload.createFromBindings(currentFocus));
	}
}

function pickNewFocus(detectedObjects) {
	// Pick the first one
	currentFocus = {
		classId: detectedObjects.classIds[0],
		label: detectedObjects.labels[0],
		box: detectedObjects.boxes[0]
	}	
}
