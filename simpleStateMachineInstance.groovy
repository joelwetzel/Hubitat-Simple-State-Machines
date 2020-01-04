/**
 *  Simple State Machine Instance
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

import groovy.json.*
	
definition(
	parent: 	"joelwetzel:Simple State Machines",
    name: 		"Simple State Machine Instance",
    namespace: 	"joelwetzel",
    author: 	"Joel Wetzel",
    description: "Child app that is instantiated by the Simple State Machines app.",
    category: 	"Convenience",
	iconUrl: 	"",
    iconX2Url: 	"",
    iconX3Url: 	"")

preferences {
	page(name: "mainPage")
}


def installed() {
	log.info "Installed with settings: ${settings}"

	initialize()
}


def initialize() {
    atomicState.internalUiState = "default"
    
    atomicState.transitionNames = []
    atomicState.dictTransitionEvent = [:]
    atomicState.dictTransitionFrom = [:]
    atomicState.dictTransitionTo = [:]
}


def updated() {
	log.info "Updated with settings: ${settings}"

    // Subscribe to events.  (This is our state machine events, not groovy events.)
	unsubscribe()
    def childEvents = enumerateEvents()
    childEvents.each {
        subscribe(it, "pushed", eventHandler)
    }
	
    atomicState.internalUiState = "default"
}





def mainPage() {
	dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if (!app.label) {
			app.updateLabel(app.name)
		}
		section(getFormat("title", (app?.label ?: app?.name).toString())) {
			input(name:	"nameOverride", type: "string", title: "State Machine Name", multiple: false, required: true, submitOnChange: true)
            
			if (settings.nameOverride) {
				app.updateLabel(settings.nameOverride)
			}
		}
        
        if (settings.nameOverride && settings.nameOverride.size() > 0) {
		    section("<b>States</b>", hideable: true, hidden: false) {
                // If they've chosen a dropdown value, delete the state
                if (settings.stateToDeleteId) {
                    log.debug "Removing State: ${settings.stateToDeleteId}"
                    deleteChildDevice(settings.stateToDeleteId)
                    app.removeSetting("stateToDeleteId")
                    atomicState.internalUiState = "default"
                }

                // List out the existing child states
                enumerateStates().each {
                    def currentStateDecorator = ""
                    if (it.displayName.toString() == atomicState.currentState) {
                        currentStateDecorator = "*"
                    }
                    paragraph "${it.displayName.toString()} ${currentStateDecorator}"
                }
            
                if (atomicState.internalUiState == "default") {
                    input "btnCreateState", "button", title: "Add State", submitOnChange: true
                }
            
                if (atomicState.internalUiState == "creatingState") {
                    input(name: "newStateName", type: "text", title: "New State Name", submitOnChange: true)
                    
                    if (newStateName) {
                        input "btnCreateStateSubmit", "button", title: "Submit", submitOnChange: true
                    }
                    input "btnCreateStateCancel", "button", title: "Cancel", submitOnChange: true
                }
            
                if (atomicState.internalUiState == "default" && enumerateStates().size() > 0) {
                    input "btnDeleteState", "button", title: "Remove State", submitOnChange: true
                }
                
                if (atomicState.internalUiState == "deletingState") {
                    // Build a list of the children for use by the dropdown
                    def existingStateOptions = []
		    	    enumerateStates().each {
			    	    existingStateOptions << [(it.deviceNetworkId.toString()): it.displayName]
    			    }

                    input(name:	"stateToDeleteId",	type: "enum", title: "Remove a state", multiple: false, required: false, submitOnChange: true, options: (existingStateOptions))
                    input "btnDeleteStateCancel", "button", title: "Cancel", submitOnChange: true
                }
            }
		}
        
        
        if (settings.nameOverride && settings.nameOverride.size() > 0) {
		    section("<b>Events</b>", hideable: true, hidden: false) {
                // If they've chosen a dropdown value, delete the event
                if (settings.eventToDeleteId) {
                    log.debug "Removing Event: ${settings.eventToDeleteId}"
                    deleteChildDevice(settings.eventToDeleteId)
                    app.removeSetting("eventToDeleteId")
                    atomicState.internalUiState = "default"
                }

                // List out the existing child events
                enumerateEvents().each {
                    paragraph "${it.displayName.toString()}"
                }
            
                if (atomicState.internalUiState == "default") {
                    input "btnCreateEvent", "button", title: "Add Event", submitOnChange: true
                }
            
                if (atomicState.internalUiState == "creatingEvent") {
                    input(name: "newEventName", type: "text", title: "New Event Name", submitOnChange: true)
                    
                    if (newEventName) {
                        input "btnCreateEventSubmit", "button", title: "Submit", submitOnChange: true
                    }
                    input "btnCreateEventCancel", "button", title: "Cancel", submitOnChange: true
                }
            
                if (atomicState.internalUiState == "default" && enumerateEvents().size() > 0) {
                    input "btnDeleteEvent", "button", title: "Remove Event", submitOnChange: true
                }
                
                if (atomicState.internalUiState == "deletingEvent") {
                    // Build a list of the children for use by the dropdown
                    def existingEventOptions = []
		    	    enumerateEvents().each {
			    	    existingEventOptions << [(it.deviceNetworkId.toString()): it.displayName]
    			    }

                    input(name:	"eventToDeleteId",	type: "enum", title: "Remove an event", multiple: false, required: false, submitOnChange: true, options: (existingEventOptions))
                    input "btnDeleteEventCancel", "button", title: "Cancel", submitOnChange: true
                }
            }
		}
        
        
        if (settings.nameOverride && settings.nameOverride.size() > 0) {
		    section("<b>Transitions</b>", hideable: true, hidden: false) {
                // If they've chosen a dropdown value, delete the transition
                if (settings.transitionToDeleteId) {
                    log.debug "Removing Transition: ${settings.transitionToDeleteId}"
                    removeTransition(settings.transitionToDeleteId)
                    app.removeSetting("transitionToDeleteId")
                    atomicState.internalUiState = "default"
                }

                // List out the existing transitions
                enumerateTransitions().each {
                    paragraph "${it}"
                }
            
                if (atomicState.internalUiState == "default") {
                    input "btnCreateTransition", "button", title: "Add Transition", submitOnChange: true
                }
            
                if (atomicState.internalUiState == "creatingTransition") {
                    // Build a list of event options to trigger the transition
                    def eventOptions = []
                    enumerateEvents().each {
                        eventOptions << [(it.displayName.toString()): it.displayName]   
                    }
                    
                    // Build a list of state options for the "from" and "to" dropdowns.
                    def existingStateOptions = []
    		    	enumerateStates().each {
			    	    existingStateOptions << [(it.displayName.toString()): it.displayName]
    			    }
                    
                    input(name:	"triggerEvent",	type: "enum", title: "Trigger Event", multiple: false, required: false, submitOnChange: true, options: (eventOptions))
                    input(name:	"fromState",	type: "enum", title: "From State", multiple: false, required: false, submitOnChange: true, options: (existingStateOptions))
                    input(name:	"toState",	type: "enum", title: "To State", multiple: false, required: false, submitOnChange: true, options: (existingStateOptions))
                    
                    if (triggerEvent && fromState && toState) {
                        input "btnCreateTransitionSubmit", "button", title: "Submit", submitOnChange: true
                    }
                    input "btnCreateTransitionCancel", "button", title: "Cancel", submitOnChange: true
                }
            
                if (atomicState.internalUiState == "default" && enumerateTransitions().size() > 0) {
                    input "btnDeleteTransition", "button", title: "Remove Transition", submitOnChange: true
                }
                
                if (atomicState.internalUiState == "deletingTransition") {
                    // Build a list of the children for use by the dropdown
                    def existingTransitionOptions = []
		    	    enumerateTransitions().each {
			    	    existingTransitionOptions << [(it): it]
    			    }

                    input(name:	"transitionToDeleteId",	type: "enum", title: "Remove a transition", multiple: false, required: false, submitOnChange: true, options: (existingTransitionOptions))
                    input "btnDeleteTransitionCancel", "button", title: "Cancel", submitOnChange: true
                }
            }
		}
        
		section () {
			input(name:	"enableLogging", type: "bool", title: "Enable Debug Logging?", defaultValue: false,	required: true)
		}
	}
}


def appButtonHandler(btn) {
    switch (btn) {
        case "btnCreateState":
            app.removeSetting("newStateName")
            atomicState.internalUiState = "creatingState"
            break
        case "btnCreateStateSubmit":
            def nsn = settings.newStateName
            atomicState.internalUiState = "default"
            log.debug "Creating state: ${nsn}"
            def newChildDevice = addChildDevice("hubitat", "Virtual Switch", "${settings.nameOverride};State;${nsn}", null, [name: "${settings.nameOverride} - ${nsn}", label: nsn, completedSetup: true, isComponent: true])
            if (!atomicState.currentState) {
                atomicState.currentState = nsn
                newChildDevice.on()
            }
            break
        case "btnCreateStateCancel":
            atomicState.internalUiState = "default"
            break
        case "btnDeleteState":
            atomicState.internalUiState = "deletingState"
            break
        case "btnDeleteStateCancel":
            atomicState.internalUiState = "default"
            break
        
        
        case "btnCreateEvent":
            app.removeSetting("newEventName")
            //app.removeSetting("fromState")
            //app.removeSetting("toState")
            atomicState.internalUiState = "creatingEvent"
            break
        case "btnCreateEventSubmit":
            def nen = settings.newEventName
            atomicState.internalUiState = "default"
            log.debug "Creating event: ${nen}"
            def newChildDevice = addChildDevice("hubitat", "Virtual Button", "${settings.nameOverride};Event;${nen}", null, [name: "${settings.nameOverride} - ${nen}", label: nen, completedSetup: true, isComponent: true])
            break
        case "btnCreateEventCancel":
            atomicState.internalUiState = "default"
            break
        case "btnDeleteEvent":
            atomicState.internalUiState = "deletingEvent"
            break
        case "btnDeleteEventCancel":
            atomicState.internalUiState = "default"
            break
        
        
        case "btnCreateTransition":
            app.removeSetting("triggerEvent")
            app.removeSetting("fromState")
            app.removeSetting("toState")
            atomicState.internalUiState = "creatingTransition"
            break
        case "btnCreateTransitionSubmit":
            atomicState.internalUiState = "default"
            defineTransition(settings.triggerEvent, settings.fromState, settings.toState)
            break
        case "btnCreateTransitionCancel":
            atomicState.internalUiState = "default"
            break
        case "btnDeleteTransition":
            atomicState.internalUiState = "deletingTransition"
            break
        case "btnDeleteTransitionCancel":
            atomicState.internalUiState = "default"
            break
    }
}




def eventHandler(evt) {
    def eventName = evt.getDevice().toString()
    def currentState = atomicState.currentState
    
    log.debug "Event Triggered: ${eventName}.  Current state: ${currentState}"   
    
    def finalState = currentState
    
    atomicState.transitionNames.each {
        // Parse the transitionNames
        def split1 = it.split(";")
        def tEvent = split1[0]
        def split2 = split1[1].split("->")
        def tFrom = split2[0]
        def tTo = split2[1]
        
        // Do we need to make this transition?
        if (eventName == tEvent &&
            currentState == tFrom) {
            finalState = tTo
        }
    }
    
    if (finalState != currentState) {
        log.debug "Transitioning: '${currentState}' -> '${finalState}'"
        
        getChildDevice("${settings.nameOverride};State;${currentState}").off()
        getChildDevice("${settings.nameOverride};State;${finalState}").on()
        atomicState.currentState = finalState
    }
}





// ***********************
// Utility Methods
// ***********************

def removeTransition(transitionName) {
    def names = atomicState.transitionNames
    names.remove(transitionName)
    atomicState.transitionNames = names
    
    def dictEvent = atomicState.dictTransitionEvent
    dictEvent.remove(transitionName)
    atomicState.dictTransitionEvent = dictEvent
    
    def dictFrom = atomicState.dictTransitionFrom
    dictFrom.remove(transitionName)
    atomicState.dictTransitionFrom = dictFrom
    
    def dictTo = atomicState.dictTransitionTo
    dictTo.remove(transitionName)
    atomicState.dictTransitionTo = dictTo
}

def defineTransition(eventName, fromId, toId) {
    def transitionName = "${eventName};${fromId}->${toId}"
    
    log.debug "Creating transition: ${transitionName}"
    
    def names = atomicState.transitionNames
    names << transitionName
    atomicState.transitionNames = names
    
    setTransitionEvent(transitionName, eventName)
    setTransitionFrom(transitionName, fromId)
    setTransitionTo(transitionName, toId)
}


def setTransitionEvent(transitionName, eventName) {
    def dict = atomicState.dictTransitionEvent
    
    dict[transitionName] = eventName
    
    atomicState.dictTransitionEvent = dict
}


def setTransitionFrom(transitionName, fromId) {
    def dict = atomicState.dictTransitionFrom
    
    dict[transitionName] = fromId
    
    atomicState.dictTransitionFrom = dict
}


def setTransitionTo(transitionName, toId) {
    def dict = atomicState.dictTransitionTo
    
    dict[transitionName] = toId
    
    atomicState.dictTransitionTo = dict
}


def getChildDevicesInCreationOrder() {
	def unorderedChildDevices = getChildDevices()
	
	def orderedChildDevices = unorderedChildDevices.sort{a,b -> a.device.id <=> b.device.id}
	
	return orderedChildDevices
}

def enumerateStates() {
    def childStates = []
    
    getChildDevicesInCreationOrder().each {
        if (it.deviceNetworkId.contains(";State;")) {
            childStates << it
        }
    }
            
    return childStates
}


def enumerateEvents() {
    def childEvents = []
    
    getChildDevicesInCreationOrder().each {
        if (it.deviceNetworkId.contains(";Event;")) {
            childEvents << it
        }
    }
            
    return childEvents
}


def enumerateTransitions() {
    return atomicState.transitionNames
}


def getFormat(type, myText="") {
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def log(msg) {
	if (enableLogging) {
		log.debug msg
	}
}






















