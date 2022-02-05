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
 **/
definition(parent: 'joelwetzel:Simple State Machines',
        name: 'Simple State Machine Instance',
        namespace: 'joelwetzel',
        author: "Joel Wetzel",
        description: 'Child app that is instantiated by the Simple State Machines app.',
        category: 'Convenience',
        iconUrl: '',
        iconX2Url: '',
        iconX3Url: '')

preferences {
    page(name: 'mainPage')
}


def installed() {
    log.info "Installed with settings: ${settings}"

    initialize()
}


def initialize() {
    setDefaultState()

    atomicState.transitions = []
}


def updated() {
    log.info "Updated with settings: ${settings}"

    bindEvents()

    setDefaultState()
}


def bindEvents() {
    // Subscribe to events.  (These are our state machine events, NOT groovy events.)
    unsubscribe()
    def childEvents = enumerateEvents()
    childEvents.each {
        subscribe(it, 'pushed', eventHandler)
    }
}


def mainPage() {
    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        if (!app.label) {
            app.updateLabel(app.name)
        }
        def updateRequired = false
//        if (true) {
        if (isInstalled() && atomicState.containsKey('transitionNames') && atomicState.transitionNames.size() > 0) {
            updateRequired = true
            section('<b>The old transitionNames state is deprecated in favor of the transitions state</b>') {
                input 'btnInternalUpdate', 'button', title: 'Update internal state', width: 3, submitOnChange: true
                input 'btnInternalUpdateCancel', 'button', title: 'Cancel', width: 3, submitOnChange: true
            }
        }
//        }
        if (!updateRequired) {
            //@TODO maybe add a button to rename the state machine if it already had a name
            section(getFormat('title', (app?.label ?: app?.name).toString())) {
                input(name: 'stateMachineName', type: 'string', title: 'State Machine Name', multiple: false, required: true, submitOnChange: true)

                if (settings.stateMachineName) {
                    //@TODO rename the child devices
                    app.updateLabel(settings.stateMachineName)
                }
            }

            if (isInstalled()) {
                statesSection()
                eventsSection()
                transitionsSection()
            }

            section() {
                input(name: 'enableLogging', type: 'bool', title: 'Enable Debug Logging?', defaultValue: true, required: true)
                if (!inDefaultState() && isInstalled()) {
                    input 'btnResetToDefaultState', 'button', title: 'Reset to default state', width: 3, submitOnChange: true
                }
            }

        }
    }
}

private boolean isInstalled() {
    app.getInstallationState() == 'COMPLETE'
}

private transitionsSection() {
    section('<b>Transitions</b>', hideable: true, hidden: false) {
        // If they've chosen a dropdown value, delete the transition
        if (settings.transitionToDeleteId) {

            log.info "Removing Transition: ${settings.transitionToDeleteId}"
            removeTransition(settings.transitionToDeleteId)
            app.removeSetting('transitionToDeleteId')
            setDefaultState()
        }

        // List out the existing transitions
        paragraph generateTransitionTable()

        if (inDefaultState() && hasMultipleStates() && hasEvents()) {
            input 'btnCreateTransition', 'button', title: 'Add Transition', width: 3, submitOnChange: true
        }

        if (atomicState.internalUiState == 'creatingTransition') {
            // Build a list of event options to trigger the transition
            def eventOptions = eventOptionsByName()

            // Build a list of state options for the "from" and "to" dropdowns.
            def existingStateOptions = stateOptionsByName()

            input(name: 'triggerEvent', type: 'enum', title: 'Trigger Event', multiple: false, required: false, submitOnChange: true, options: (eventOptions))
            input(name: 'fromState', type: 'enum', title: 'From State', width: 6, multiple: false, required: false, submitOnChange: true, options: (existingStateOptions))
            input(name: 'toState', type: 'enum', title: 'To State', width: 6, multiple: false, required: false, submitOnChange: true, options: (existingStateOptions))

            if (triggerEvent && fromState && toState) {
                input 'btnCreateTransitionSubmit', 'button', title: 'Submit', width: 3, submitOnChange: true
            }
            input 'btnCreateTransitionCancel', 'button', title: 'Cancel', width: 3, submitOnChange: true
        }

        if (inDefaultState() && hasTransitions()) {
            input 'btnDeleteTransition', 'button', title: 'Remove Transition', width: 3, submitOnChange: true
        }

        if (atomicState.internalUiState == 'deletingTransition') {
            // Build a list of the children for use by the dropdown
            def existingTransitionOptions = transitionOptions()

            input(name: 'transitionToDeleteId', type: 'enum', title: 'Remove a transition', multiple: false, required: false, submitOnChange: true, options: (existingTransitionOptions))
            input 'btnDeleteTransitionCancel', 'button', title: 'Cancel', submitOnChange: true
        }
    }
}

private List<LinkedHashMap<String, Object>> eventOptionsByName() {
    enumerateEvents().collect { [(it.displayName.toString()): it.displayName] }
}

private List<LinkedHashMap<String, Object>> stateOptionsByName() {
    enumerateStates().collect { [(it.displayName.toString()): it.displayName] }
}

private List<LinkedHashMap<Object, Object>> transitionOptions() {
    enumerateTransitions().collect {[(it.name): it.name] }
}

private boolean inDefaultState() {
    atomicState.internalUiState == 'default'
}

private eventsSection() {
    section('<b>Events</b>', hideable: true, hidden: false) {
        // If they've chosen a dropdown value, delete the event
        if (settings.eventToDeleteId) {
            log.info "Removing Event: ${settings.eventToDeleteId}"
            def device = getChildDevice(settings.eventToDeleteId)
            def name = device.getLabel()
            removeTransitionsForEvent(name)
            deleteChildDevice(settings.eventToDeleteId)
            app.removeSetting('eventToDeleteId')
            setDefaultState()
        }

        // List out the existing child events
        enumerateEvents().each {
            paragraph "${it.displayName.toString()}"
        }

        if (inDefaultState()) {
            input 'btnCreateEvent', 'button', title: 'Add Event', width: 3, submitOnChange: true
        }

        if (atomicState.internalUiState == 'creatingEvent') {
            input(name: 'newEventName', type: 'text', title: 'New Event Name', submitOnChange: true)

            if (newEventName) {
                input 'btnCreateEventSubmit', 'button', title: 'Submit', width: 3, submitOnChange: true
            }
            input 'btnCreateEventCancel', 'button', title: 'Cancel', width: 3, submitOnChange: true
        }

        if (inDefaultState() && hasEvents()) {
            input 'btnDeleteEvent', 'button', title: 'Remove Event', width: 3, submitOnChange: true
        }

        if (atomicState.internalUiState == 'deletingEvent') {
            // Build a list of the children for use by the dropdown
            def existingEventOptions = eventOptionsByDNI()

            input(name: 'eventToDeleteId', type: 'enum', title: 'Remove an event', multiple: false, required: false, submitOnChange: true, options: (existingEventOptions))
            input 'btnDeleteEventCancel', 'button', title: 'Cancel', submitOnChange: true
        }
        if (inDefaultState() && hasEvents()) {
            input 'btnRenameEvent', 'button', title: 'Rename Event', width: 2, submitOnChange: true
        }
        if (atomicState.internalUiState == 'renamingEvent') {
            def existingEventOptions = eventOptionsByDNI()

            input(name: 'eventToRenameId', type: 'enum', title: 'Rename an event', multiple: false, required: false, submitOnChange: true, options: (existingEventOptions))
            input(name: "newEventName", type: 'text', title: 'New Event Name', submitOnChange: true)
            if (newStateName) {
                input 'btnRenameEventSubmit', 'button', title: 'Submit', width: 2, submitOnChange: true
            }
            input 'btnRenameEventCancel', 'button', title: 'Cancel', width: 2, submitOnChange: true
        }
    }
}

private List<LinkedHashMap<String, Object>> eventOptionsByDNI() {
    enumerateEvents().collect { [(it.deviceNetworkId.toString()): it.displayName] }
}

private statesSection() {
    section('<b>States</b>', hideable: true, hidden: false) {
        // If they've chosen a dropdown value, delete the state
        if (settings.stateToDeleteId) {
            log.info "Removing State: ${settings.stateToDeleteId}"
            def device = getChildDevice(settings.stateToDeleteId)
            def name = device.getLabel()
            removeTransitionsForState(name)
            deleteChildDevice(settings.stateToDeleteId)
            app.removeSetting('stateToDeleteId')

            setDefaultState()
        }

        // List out the existing child states
        enumerateStates().each {
            def currentStateDecorator = it.displayName.toString() == atomicState.currentState ? '(ACTIVE)' : ''
            paragraph "${it.displayName.toString()} ${currentStateDecorator}"
        }

        if (inDefaultState()) {
            input 'btnCreateState', 'button', title: 'Add State', width: 3, submitOnChange: true
        }

        if (atomicState.internalUiState == 'creatingState') {
            input(name: 'newStateName', type: 'text', title: 'New State Name', submitOnChange: true)

            if (newStateName) {
                input 'btnCreateStateSubmit', 'button', title: 'Submit', width: 3, submitOnChange: true
            }
            input 'btnCreateStateCancel', 'button', title: 'Cancel', width: 3, submitOnChange: true
        }

        if (inDefaultState() && hasStates()) {
            input 'btnDeleteState', 'button', title: 'Remove State', width: 3, submitOnChange: true
        }

        if (atomicState.internalUiState == 'deletingState') {
            // Build a list of the children for use by the dropdown
            def existingStateOptions = stateOptionsByDNI()

            input(name: 'stateToDeleteId', type: 'enum', title: 'Remove a state', multiple: false, required: false, submitOnChange: true, options: (existingStateOptions))
            input 'btnDeleteStateCancel', 'button', title: 'Cancel', submitOnChange: true
        }
        if (atomicState.internalUiState == "default" && hasStates()) {
            input "btnRenameState", "button", title: "Rename State", width: 2, submitOnChange: true
        }
        if (atomicState.internalUiState == "renamingState") {
            def existingStateOptions = stateOptionsByDNI()

            input(name: 'stateToRenameId', type: 'enum', title: 'Rename a state', multiple: false, required: false, submitOnChange: true, options: (existingStateOptions))
            input(name: 'newStateName', type: 'text', title: 'New State Name', submitOnChange: true)
            if (newStateName) {
                input 'btnRenameStateSubmit', 'button', title: 'Submit', width: 2, submitOnChange: true
            }
            input 'btnRenameStateCancel', 'button', title: 'Cancel', width: 2, submitOnChange: true
        }
    }
}

private List<LinkedHashMap<String, Object>> stateOptionsByDNI() {
    enumerateStates().collect { [(it.deviceNetworkId.toString()): it.displayName] }
}

private boolean hasStates() {
    enumerateStates().size() > 0
}

private boolean hasEvents() {
    enumerateEvents().size() >= 1
}

private boolean hasMultipleStates() {
    enumerateStates().size() >= 2
}

private boolean hasTransitions() {
    enumerateTransitions().size() > 0
}


def appButtonHandler(btn) {
    switch (btn) {
        case 'btnInternalUpdate':
            atomicState.transitions = enumerateTransitionNames().collect { parseTransitionName(it) }
            atomicState.remove('transitionNames')
            break
        case 'btnCreateState':
            app.removeSetting('newStateName')
            atomicState.internalUiState = 'creatingState'
            break
        case 'btnCreateStateSubmit':
            def nsn = "State;${settings.stateMachineName};${settings.newStateName}"
            setDefaultState()
            log.info "Creating state: ${settings.newStateName}"
            def newChildDevice = addChildDevice('joelwetzel', 'SSM State', nsn, null, [name: nsn, label: settings.newStateName, completedSetup: true, isComponent: true])
            if (!atomicState.currentState) {
                atomicState.currentState = settings.newStateName
                newChildDevice._on()
            }
            break
        case 'btnDeleteState':
            atomicState.internalUiState = 'deletingState'
            break
        case 'btnCreateEvent':
            app.removeSetting('newEventName')
            atomicState.internalUiState = 'creatingEvent'
            break
        case 'btnCreateEventSubmit':
            def nen = "Event;${settings.stateMachineName};${settings.newEventName}"
            setDefaultState()
            log.info "Creating event: ${settings.newEventName}"
            def newChildDevice = addChildDevice('joelwetzel', 'SSM Event', nen, null, [name: nen, label: settings.newEventName, completedSetup: true, isComponent: true])
            bindEvents()
            break
        case 'btnDeleteEvent':
            atomicState.internalUiState = 'deletingEvent'
            break
        case 'btnCreateTransition':
            app.removeSetting('triggerEvent')
            app.removeSetting('fromState')
            app.removeSetting('toState')
            atomicState.internalUiState = 'creatingTransition'
            break
        case 'btnCreateTransitionSubmit':
            setDefaultState()
            defineTransition(settings.triggerEvent, settings.fromState, settings.toState)
            break
        case 'btnDeleteTransition':
            atomicState.internalUiState = 'deletingTransition'
            break
        case "btnRenameState":
            app.removeSetting("newStateName")
            app.removeSetting('stateToRenameId')
            atomicState.internalUiState = "renamingState"
            break
        case "btnRenameStateSubmit":
            def nsn = makeName('State', settings.newStateName)
            if (settings.stateToRenameId) {
                log.debug "Renaming state ${settings.stateToRenameId} to ${nsn}"
                def device = getChildDevice(settings.stateToRenameId)
                if (device) {
                    String oldLabel = device.getLabel()
                    String oldName = device.getName()
                    String newLabel = settings.newStateName
                    String newName = makeName('State', newLabel)
                    device.setName(newName)
                    device.setDeviceNetworkId(newName)
                    device.setLabel(newLabel)
                    def transitions = enumerateTransitions()
                    def newTransitions = updateTransitions(oldLabel, newLabel, transitions, ['from', 'to'])
                    atomicState.transitions = newTransitions
                } else {
                    log.debug 'No device'
                }
            } else {
                log.debug "No state to rename"
            }
            setDefaultState()
            break
        case "btnRenameEvent":
            app.removeSetting("newEventName")
            app.removeSetting('eventToRenameId')
            atomicState.internalUiState = "renamingEvent"
            break
        case "btnRenameEventSubmit":
            def nsn = makeName('Event', settings.newEventName)
            if (settings.eventToRenameId) {
                log.debug "Renaming event ${settings.eventToRenameId} to ${nsn}"
                def device = getChildDevice(settings.eventToRenameId)
                if (device) {
                    String oldLabel = device.getLabel()
                    String oldName = device.getName()
                    String newLabel = settings.newEventName
                    String newName = makeName('Event', newLabel)
                    device.setName(newName)
                    device.setDeviceNetworkId(newName)
                    device.setLabel(newLabel)
                    def transitions = enumerateTransitions()
                    def newTransitions = updateTransitions(oldLabel, newLabel, transitions, ['event'])
                    atomicState.transitions = newTransitions
                } else {
                    log.debug 'No device'
                }
            } else {
                log.debug "No event to rename"
            }
            setDefaultState()
            break
        case 'btnCreateStateCancel':
        case 'btnDeleteStateCancel':
        case 'btnDeleteTransitionCancel':
        case 'btnResetToDefaultState':
        case 'btnCreateTransitionCancel':
        case 'btnDeleteEventCancel':
        case 'btnCreateEventCancel':
        case "btnRenameStateCancel":
        case "btnRenameEventCancel":
            setDefaultState()
            break
    }
}

private void setDefaultState() {
    atomicState.internalUiState = 'default'
}

def eventHandler(evt) {
    def eventName = evt.getDevice().toString()
    def currentState = atomicState.currentState

    log "Event Triggered: ${eventName}.  Current state: ${currentState}"

    def finalState = currentState

    enumerateTransitions().each {
        log.debug "***** ${it}"

        // Do we need to make this transition? ROB: should this continue after setting finalState?
        if (eventName == it.event && currentState == it.from) {
            finalState = it.to
        }
    }

    if (finalState != currentState) {
        log.info "Transitioning: '${currentState}' -> '${finalState}'"

        getChildDevice("State;${settings.stateMachineName};${currentState}")._off()
        getChildDevice("State;${settings.stateMachineName};${finalState}")._on()
        atomicState.currentState = finalState
    }
}


// ***********************
// Utility Methods
// ***********************

def removeTransition(transitionName) {
    def transitions = enumerateTransitions()
    def newTransitions = transitions.findAll { it.name != transitionName }
    atomicState.transitions = newTransitions
}

def defineTransition(eventName, fromId, toId) {
    def transition = [name: "${eventName};${fromId}->${toId}", event: eventName, from: fromId, to: toId]

    log.info "Creating transition: ${transition.name}}"

    def transitions = atomicState.transitions

    transitions << transition
    atomicState.transitions = transitions
}


def getChildDevicesInCreationOrder() {
    def unorderedChildDevices = getChildDevices()

    def orderedChildDevices = unorderedChildDevices.sort { a, b -> a.device.id <=> b.device.id }

    return orderedChildDevices
}


def enumerateStates() {
    getChildDevicesInCreationOrder().findAll {it.deviceNetworkId.startsWith('State;') }
}


def enumerateEvents() {
    getChildDevicesInCreationOrder().findAll {it.deviceNetworkId.startsWith('Event;') }
}


def List<String> enumerateTransitionNames() {
    atomicState.transitionNames
}


def getFormat(type, myText = '') {
    if (type == 'header-green') return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if (type == 'line') return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
    if (type == 'title') return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def log(msg) {
    if (enableLogging) {
        log.debug msg
    }
}


def generateTransitionTable() {
    def states = enumerateStates()
    def transitions = enumerateTransitions()

    def stateCount = states.size()
    def tableSize = stateCount + 1

    // Initialize the table data
    def cellValues = new String[tableSize][tableSize]
    for (int i = 0; i < tableSize; i++) {
        for (int j = 0; j < tableSize; j++) {
            cellValues[i][j] = ''
        }
    }

    // Add the headings
    for (int i = 0; i < stateCount; i++) {
        cellValues[i + 1][0] = '<b>' + states[i].displayName + '</b>'
        cellValues[0][i + 1] = '<b>' + states[i].displayName + '</b>'
    }

    // Create a reverse lookup from state name to index
    def stateIndices = [:]
    for (int i = 0; i < stateCount; i++) {
        stateIndices[states[i].displayName.toString()] = i
    }

    // Put each transition in a cell
    transitions.each {
        if ((null != stateIndices[it.from]) && (null != stateIndices[it.to])) {
            def oldCellValue = cellValues[stateIndices[it.from] + 1][stateIndices[it.to] + 1]
            def newCellValue = it.event + (oldCellValue ? '<br>' + oldCellValue : '')
            // If more than one event causes the same transition, we need to show both in the cell.
            cellValues[stateIndices[it.from] + 1][stateIndices[it.to] + 1] = newCellValue
        }
    }

    // List out the transitions for the left-hand side
    def listStr = ''
    for (transition in transitions) {
        listStr += transition.name + '<br>'
    }

    // Render the table into HTML
    def table = '<table border=1>'
    for (int i = 0; i < tableSize; i++) {
        table += '<tr>'

        for (int j = 0; j < tableSize; j++) {
            table += "<td>${cellValues[i][j]}</td>"
        }

        table += '</tr>'
    }
    table += '</table>'

    def fullHtml = "<table width=100%><tr><td valign=top>${listStr}</td></tr><tr><td>${table}</td></tr></table>"

    if (stateCount >= 2) {
        return fullHtml
    } else {
        return ''
    }

}

private List<Map> enumerateTransitions() {
    if (atomicState.transitions) {
        atomicState.transitions
    } else {
        enumerateTransitionNames().collect { parseTransitionName(it) }
    }
}

private static Map<String, String> parseTransitionName(String it) {
    def split1 = it.split(';')
    def tEvent = split1[0]
    def split2 = split1[1].split('->')
    def tFrom = split2[0]
    def tTo = split2[1]
    [name: it, event: tEvent, from: tFrom, to: tTo]
}


private GString makeName(String kind, String name) {
    "${kind};${settings.stateMachineName};${name}"
}

private static List<Map> updateTransitions(String oldName, String newName, List<Map> transitions, List<String> keys) {
    transitions.collect { updateNameInTransition(oldName, newName, it, keys) }
}

private static Map updateNameInTransition(String oldName, String newName, Map transition, List<String> keys) {
    for (key in keys) {
        if (transition[key] == oldName) {
            transition.put(key, newName) // replace the name
            transition.put('name', "${transition.event};${transition.from}->${transition.to}") // update the key
        }
    }
    transition
}

def void removeTransitionsForState(String name) {
    def newTransitions = enumerateTransitions().findAll {it.from != name && it.to != name}
    atomicState.transitions = newTransitions
}

def void removeTransitionsForEvent(String name) {
    def newTransitions = enumerateTransitions().findAll {it.event != name }
    atomicState.transitions = newTransitions
}