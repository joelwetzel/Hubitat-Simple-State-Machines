/**
 *  State for Simple State Machines
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
        definition (name: "SSM State", namespace: "joelwetzel", author: "Joel Wetzel") {
        capability "Actuator"
        capability "Switch"
        capability "Sensor"
    }
    
    preferences {
        input(name:	"enableLogging", type: "bool", title: "Enable Debug Logging?", defaultValue: true,	required: true)
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
    sendEvent(name: "switch", value: "off", isStateChange: true)
}

def updated() {
}

def _on() {
    log "State '${device.displayName}' activating."
    sendEvent(name: "switch", value: "on", isStateChange: true)
}

def _off() {
    log "State '${device.displayName}' deactivating."
    sendEvent(name: "switch", value: "off", isStateChange: true)
}

def on() {
    // Change nothing.  States can only be activated/deactivated by the state machine that created them.
    sendEvent(name: "switch", value: device.currentValue("switch"), isStateChange: false)
}

def off() {
    // Change nothing.  States can only be activated/deactivated by the state machine that created them.
    sendEvent(name: "switch", value: device.currentValue("switch"), isStateChange: false)
}
