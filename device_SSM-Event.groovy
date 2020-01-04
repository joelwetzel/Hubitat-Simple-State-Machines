/**
 *  Event for Simple State Machines
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
    definition (name: "SSM Event", namespace: "joelwetzel", author: "Joel Wetzel") {
        capability "Actuator"
        capability "PushableButton"
        capability "Sensor"
        capability "Configuration"
            
        command "push"
    }
    
    preferences {
        input(name:	"enableLogging", type: "bool", title: "Enable Debug Logging?", defaultValue: false,	required: true)
    }
}

def log(msg) {
	if (enableLogging) {
		log.debug msg
	}
}

def parse(String description) {
}

def installed() {
    configure()
}

def updated() {
    configure()
}

def configure() {
    sendEvent(name: "numberOfButtons", value: 1, displayed: false)
}

def push(index) {
    if (!index) {
        index = 1
    }
    
    log "pushed: ${index}"
    sendEvent(name: "pushed", value: index, isStateChange: true)
}


