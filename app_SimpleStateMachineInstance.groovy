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
import groovy.transform.Field

import java.util.regex.Pattern

definition(
        parent: 'joelwetzel:Simple State Machines',
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
    makeContainers()
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

boolean isContainerised() { getStateContainer() != null || getEventContainer() != null }

def mainPage() {
    def hasOldStyleStates = (isInstalled() && (!isContainerised()) && (hasStates() || hasEvents()))

    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        if (!app.label) {
            app.updateLabel(app.name)
        }
        def transitionUpdateRequired = isInstalled() && atomicState.containsKey('transitionNames') && atomicState.transitionNames.size() > 0
        if (transitionUpdateRequired) {
            section('<strong>The old transitionNames state is deprecated in favor of the transitions state</strong>') {
                input 'btnInternalUpdate', 'button', title: 'Update internal state', width: 3, submitOnChange: true
                input 'btnInternalUpdateCancel', 'button', title: 'Cancel', width: 3, submitOnChange: true
            }
        } else {
            section(getFormat('title', (app?.label ?: app?.name).toString())) {
                input(name: 'stateMachineName', type: 'string', title: 'State Machine Name', multiple: false, required: true, submitOnChange: true)

                if (settings.stateMachineName) {
                    app.updateLabel(settings.stateMachineName)
                }
            }

            if (isInstalled()) {
                if (hasOldStyleStates) {
                    section('<strong>Old version detected</strong>') {
                        paragraph 'This state machine is using the old layout of top level states and events'
                        paragraph 'At present the state and event devices are distributed throughout the devices page which can make them difficult to track down.'
                        paragraph "It is strongly recommended to switch to the new hierarchical layout with the states and events as children of state and event containers as this makes it much easier to locate them on the devices page."
                        paragraph 'To do this, go the the <strong>Mermaid export</strong> section and copy the mermaid output text, then create a new state machine and import the mermaid text.'
                        paragraph 'Once this is done you should go through each state and event, follow the links to any apps and replace the old state and event devices with the new ones.'
                        paragraph 'At that point you should be able to disable the old state machine (this one) and test the new one. If everything works then delete this state machine instance.'
                    }
                } else if (!(hasStates() || hasEvents())) {
                    importSection()
                }
                statesSection()
                eventsSection()
                transitionsSection()
                mermaidSection()
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

        if (isInState('creatingTransition')) {
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

        if (isInState('deletingTransition')) {
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
    enumerateTransitions().collect { [(it.name): it.name] }
}

private boolean inDefaultState() {
    isInState('default')
}

private mermaidSection() {
    section('<strong>Mermaid export</strong>', hideable: true, hidden: true) {
        paragraph 'Mermaid is way of using structured text to draw various types of diagram'
        if (hasTransitions()) {
            paragraph 'Paste the following into <a href="https://mermaid.live" target="_blank">Mermaid Live Editor</a>'
            def mermaidText = "<pre>${asMermaid(enumerateTransitions())}<pre>"
            paragraph mermaidText
        }
        paragraph 'NOTE: this can also be used to transfer a Simple State Machine from one hub to another'
    }
}

private importSection() {
    def hasMermaidText = (settings.txtMermaid != null && settings.txtMermaid.trim() != '')
    section('<b>Import export', hideable: true, hidden: !hasMermaidText) {
        paragraph 'Paste the mermaid text here'
        input(name: 'txtMermaid', type: 'textarea', title: 'Mermaid text', width: 12, height: 30, submitOnChange: true)
        if (hasMermaidText) {
            input 'btnMermaidImport', 'button', title: 'Import', width: 3, submitOnChange: true
        }
    }
}

private eventsSection() {
    section('<strong>Events</strong>', hideable: true, hidden: false) {
        // If they've chosen a dropdown value, delete the event
        //@todo this is quite severe - should probably split into two stages - this should check how many transitions
        //@todo will be removed and require confirmation if there are any such transitions
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
            paragraph makeDeviceLink(it)
        }

        if (inDefaultState()) {
            input 'btnCreateEvent', 'button', title: 'Add Event', width: 3, submitOnChange: true
        }

        if (isInState('creatingEvent')) {
            input(name: 'newEventName', type: 'text', title: 'New Event Name', submitOnChange: true)

            if (newEventName) {
                input 'btnCreateEventSubmit', 'button', title: 'Submit', width: 3, submitOnChange: true
            }
            input 'btnCreateEventCancel', 'button', title: 'Cancel', width: 3, submitOnChange: true
        }

        if (inDefaultState() && hasEvents()) {
            input 'btnDeleteEvent', 'button', title: 'Remove Event', width: 3, submitOnChange: true
        }

        if (isInState('deletingEvent')) {
            // Build a list of the children for use by the dropdown
            def existingEventOptions = eventOptionsByDNI()

            input(name: 'eventToDeleteId', type: 'enum', title: 'Remove an event', multiple: false, required: false, submitOnChange: true, options: (existingEventOptions))
            input 'btnDeleteEventCancel', 'button', title: 'Cancel', submitOnChange: true
        }
        if (inDefaultState() && hasEvents()) {
            input 'btnRenameEvent', 'button', title: 'Rename Event', width: 2, submitOnChange: true
        }
        if (isInState('renamingEvent')) {
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
    section('<strong>States</strong>', hideable: true, hidden: false) {
        // If they've chosen a dropdown value, delete the state
        if (settings.stateToDeleteId) {
            def stateToDelete = settings.stateToDeleteId
            //@todo should check the number of transitions and require confirmation
            deleteState(stateToDelete)
            app.removeSetting('stateToDeleteId')

            setDefaultState()
        }

        // List out the existing child states
        enumerateStates().each {
            def currentStateDecorator = it.displayName.toString() == atomicState.currentState ? '(ACTIVE)' : ''
            paragraph makeDeviceLink(it) + " ${currentStateDecorator}"
        }

        if (inDefaultState()) {
            input 'btnCreateState', 'button', title: 'Add State', width: 3, submitOnChange: true
        }

        if (isInState('creatingState')) {
            input(name: 'newStateName', type: 'text', title: 'New State Name', submitOnChange: true)

            if (newStateName) {
                input 'btnCreateStateSubmit', 'button', title: 'Submit', width: 3, submitOnChange: true
            }
            input 'btnCreateStateCancel', 'button', title: 'Cancel', width: 3, submitOnChange: true
        }

        if (inDefaultState() && hasStates()) {
            input 'btnDeleteState', 'button', title: 'Remove State', width: 3, submitOnChange: true
        }


        if (isInState('deletingState')) {
            // Build a list of the children for use by the dropdown
            def existingStateOptions = stateOptionsByDNI()

            input(name: 'stateToDeleteId', type: 'enum', title: 'Remove a state', multiple: false, required: false, submitOnChange: true, options: (existingStateOptions))
            input 'btnDeleteStateCancel', 'button', title: 'Cancel', submitOnChange: true
        }
        if (inDefaultState() && hasStates()) {
            input "btnRenameState", "button", title: "Rename State", width: 2, submitOnChange: true
        }
        if (isInState('renamingState')) {
            def existingStateOptions = stateOptionsByDNI()

            input(name: 'stateToRenameId', type: 'enum', title: 'Rename a state', multiple: false, required: false, submitOnChange: true, options: (existingStateOptions))
            input(name: 'newStateName', type: 'text', title: 'New State Name', submitOnChange: true)
            if (settings.newStateName) {
                input 'btnRenameStateSubmit', 'button', title: 'Submit', width: 2, submitOnChange: true
            }
            input 'btnRenameStateCancel', 'button', title: 'Cancel', width: 2, submitOnChange: true
        }
        if (inDefaultState() && hasStates()) {
            input 'btnForceActive', 'button', title: 'Force active', width: 2, submitOnChange: true
        }
        if (isInState('forceActiveState')) {
            def existingStateOptions = stateOptionsByName()

            input(name: 'stateToActivate', type: 'enum', title: 'Force state active', multiple: false, required: false, submitOnChange: true, options: (existingStateOptions))
            if (settings.stateToActivate) {
                input 'btnForceActiveSubmit', 'button', title: 'Make active', width: 2, submitOnChange: true
            }
            input 'btnForceActiveCancel', 'button', title: 'Cancel', width: 2, submitOnChange: true
        }
    }
}

private boolean isInState(String theState) {
    atomicState.internalUiState == theState
}

private void deleteState(stateToDelete) {
    log.info "Removing State: ${stateToDelete}"
    def device = getChildDevice(stateToDelete)
    def name = device.getLabel()
    removeTransitionsForState(name)
    deleteChildDevice(stateToDelete)
}

private static GString makeDeviceLink(com.hubitat.app.DeviceWrapper device) {
    return "<a href=\"/device/edit/${device.getId()}\" target=\"_blank\">${device.displayName.toString()}</a>"
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

def createNewState(String newName) {
    def nsn = makeStateName(newName)
    log.info "Creating state: ${newName}"
    def theParent = getStateContainer() ?: this
    def newChildDevice = theParent.addChildDevice('joelwetzel', 'SSM State', nsn, [name: nsn, label: newName, completedSetup: true, isComponent: true])
    if (!atomicState.currentState) {
        atomicState.currentState = newName
        newChildDevice._on()
    }
}

def createEvent(String eventName, String ssmName) {
    log.info "Creating event: ${eventName}"
    def nen = makeEventName(eventName)
    def theParent = getEventContainer() ?: this
    theParent.addChildDevice('joelwetzel', 'SSM Event', nen, [name: nen, label: eventName, completedSetup: true, isComponent: true])
}

def appButtonHandler(btn) {
    switch (btn) {
        case 'btnInternalUpdate':
            atomicState.transitions = enumerateTransitionNames().collect { parseTransitionName(it) }
            atomicState.remove('transitionNames')
            break
        case 'btnCreateState':
            app.removeSetting('newStateName')
            setTheState('creatingState')
            break
        case 'btnCreateStateSubmit':
            setDefaultState()
            createNewState(settings.newStateName)
            break
        case 'btnDeleteState':
            setTheState('deletingState')
            break
        case 'btnForceActive':
            setTheState('forceActiveState')
            break
        case 'btnCreateEvent':
            app.removeSetting('newEventName')
            setTheState('creatingEvent')
            break
        case 'btnCreateEventSubmit':
            def nen = makeEventName(settings.newEventName)
            setDefaultState()
            log.info "Creating event: ${settings.newEventName}"
            createEvent(settings.newEventName, settings.stateMachineName)
//            def newChildDevice = addChildDevice('joelwetzel', 'SSM Event', nen, null, [name: nen, label: settings.newEventName, completedSetup: true, isComponent: true])
            bindEvents()
            break
        case 'btnDeleteEvent':
            setTheState('deletingEvent')
            break
        case 'btnCreateTransition':
            app.removeSetting('triggerEvent')
            app.removeSetting('fromState')
            app.removeSetting('toState')
            setTheState('creatingTransition')
            break
        case 'btnCreateTransitionSubmit':
            setDefaultState()
            defineTransition(settings.triggerEvent, settings.fromState, settings.toState)
            break
        case 'btnDeleteTransition':
            setTheState('deletingTransition')
            break
        case "btnRenameState":
            app.removeSetting('newStateName')
            app.removeSetting('stateToRenameId')
            setTheState('renamingState')
            break
        case "btnRenameStateSubmit":
            renameState()
            break
        case 'btnRenameEvent':
            app.removeSetting('newEventName')
            app.removeSetting('eventToRenameId')
            setTheState('renamingEvent')
            break
        case "btnRenameEventSubmit":
            renameEvent()
            break
        case 'btnMermaidImport':
            importMermaid(settings.txtMermaid)
            break;
        case 'btnForceActiveSubmit':
            forceStateActive(settings.stateToActivate)
            app.removeSetting('stateToActivate')
            setDefaultState()
            break;
        case 'btnCreateStateCancel':
        case 'btnDeleteStateCancel':
        case 'btnDeleteTransitionCancel':
        case 'btnResetToDefaultState':
        case 'btnCreateTransitionCancel':
        case 'btnDeleteEventCancel':
        case 'btnCreateEventCancel':
        case 'btnRenameStateCancel':
        case 'btnRenameEventCancel':
        case 'btnForceActiveCancel':
            setDefaultState()
            break
    }
}

private void renameEvent() {
    def newEventName = makeName('Event', settings.newEventName)
    if (settings.eventToRenameId) {
        log "Renaming event ${settings.eventToRenameId} to ${newEventName}"
        def device = getChildDevice(settings.eventToRenameId)
        if (device) {
            String oldLabel = device.getLabel()
            String newLabel = settings.newEventName
            device.setName(newEventName)
            device.setDeviceNetworkId(newEventName)
            device.setLabel(newLabel)
            def transitions = enumerateTransitions()
            def newTransitions = updateTransitions(oldLabel, newLabel, transitions, ['event'])
            atomicState.transitions = newTransitions
        } else {
            log 'No device'
        }
    } else {
        log "No event to rename"
    }
    setDefaultState()
}

private void renameState() {
    def newStateName = makeName('State', settings.newStateName) // this seems off
    if (settings.stateToRenameId) {
        log "Renaming state ${settings.stateToRenameId} to ${newStateName}"
        def device = getChildDevice(settings.stateToRenameId)
        if (device) {
            String oldLabel = device.getLabel()
            String newLabel = settings.newStateName
            device.setName(newStateName)
            device.setDeviceNetworkId(newStateName)
            device.setLabel(newLabel)
            if (atomicState.currentState == oldLabel) {
                atomicState.currentState = newLabel
            }
            def transitions = enumerateTransitions()
            def newTransitions = updateTransitions(oldLabel, newLabel, transitions, ['from', 'to'])
            atomicState.transitions = newTransitions
        } else {
            log 'No device'
        }
    } else {
        log "No state to rename"
    }
    setDefaultState()
}

private void setDefaultState() {
    setTheState('default')
}

private void setTheState(String theState) {
    atomicState.internalUiState = theState
}

def eventHandler(evt) {
    def eventName = evt.getDevice().toString()
    def currentState = atomicState.currentState

    log "Event Triggered: ${eventName}.  Current state: ${currentState}"

    def finalState = currentState

    enumerateTransitions().each {
        // Do we need to make this transition? ROB: should this continue after setting finalState?
        if (eventName == it.event && currentState == it.from) {
            finalState = it.to
        }
    }

    if (finalState != currentState) {
        log.info "Transitioning: '${currentState}' -> '${finalState}'"

        changeCurrentState(currentState, finalState)
    }
}

private void changeCurrentState(currentState, finalState) {
    getChildState(currentState)?._off()
    getChildState(finalState)?._on()
    atomicState.currentState = finalState
}

private getChildState(currentState) {
    def theParent = getStateContainer() ?: this
    theParent.getChildDevice(makeStateName(currentState))
}

private makeStateName(String stateName) {
    return stateName.startsWith('State;') ? stateName : "State;${settings.stateMachineName};${stateName}"
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


private def getChildDevicesInCreationOrder() {
    return orderDevices(getChildDevices())
}

private static List<com.hubitat.app.ChildDeviceWrapper> orderDevices(unorderedChildDevices) {
    def orderedChildDevices = unorderedChildDevices?.sort { a, b -> a.device.id <=> b.device.id }

    return orderedChildDevices ?: []
}


private List<com.hubitat.app.ChildDeviceWrapper> enumerateStates() {
    if (isContainerised() && (getStateContainer() != null)) {
        orderDevices(getStateContainer()?.getChildDevices())
    } else {
        getChildDevicesInCreationOrder().findAll { it.deviceNetworkId.startsWith('State;') }
    }
}


private List<com.hubitat.app.ChildDeviceWrapper> enumerateEvents() {
    if (isContainerised() && (getEventContainer() != null)) {
        orderDevices(getEventContainer()?.getChildDevices())
    } else {
        getChildDevicesInCreationOrder().findAll { it.deviceNetworkId.startsWith('Event;') }
    }
}


private List<String> enumerateTransitionNames() {
    atomicState.transitionNames
}


static def getFormat(type, myText = '') {
    if (type == 'header-green') return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if (type == 'line') return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
    if (type == 'title') return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def log(msg) {
    if (enableLogging) {
        log.debug msg
    }
}


private def generateTransitionTable() {
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

Integer countTransitionsForState(String name) {
    enumerateTransitions().count { it.from == name || it.to == name }
}

Integer countTransitionsForEvent(String name) {
    enumerateTransitions().count { it.event == name }
}

void removeTransitionsForState(String name) {
    def newTransitions = enumerateTransitions().findAll { it.from != name && it.to != name }
    atomicState.transitions = newTransitions
}

void removeTransitionsForEvent(String name) {
    def newTransitions = enumerateTransitions().findAll { it.event != name }
    atomicState.transitions = newTransitions
}

private importMermaid(String source) {
    makeContainers()
    def parts = source.split(/\n/)
    if (parts[0] != 'stateDiagram-v2') {
        return
    }
    parts.drop(0)
    def aliases = [:]
    parts.each {
        importState(it, aliases)
    }
    log aliases
    def events = []
    parts.each {
        importTransition(it, aliases)
    }
    bindEvents()
}

private void makeContainers() {
    addChildDevice('joelwetzel', 'SSM StateContainer', stateContainerName, [name: stateContainerName, completedSetup: true, isComponent: !developingUpgrade()])
    addChildDevice('joelwetzel', 'SSM EventContainer', eventContainerName, [name: eventContainerName, completedSetup: true, isComponent: !developingUpgrade()])
}

@Field statePattern = Pattern.compile(/^state\s"(?<stateName>.+)"\sas\s(?<alias>s\d{1,})$/)
@Field transitionPattern = Pattern.compile(/^(?<initial>s\d{1,})-->(?<final>s\d{1,}):(?<event>(?:\w|\s)+)$/)

def importState(String s, Map aliases) {
    s = s.trim()
    def stateDefMatcher = statePattern.matcher(s)
    if (stateDefMatcher.matches()) {
        def stateName = stateDefMatcher.group('stateName')
        def alias = stateDefMatcher.group('alias')
        log "Potential import state: (${stateName} as ${alias}"
        aliases[alias] = stateName
        createNewState(stateName)
    }
}

def importTransition(String s, Map aliases) {
    s = s.trim()
    def transitionMatcher = transitionPattern.matcher(s)
    if (transitionMatcher.matches()) {
        def initialStateAlias = transitionMatcher.group('initial')
        def finalStateAlias = transitionMatcher.group('final')
        def eventName = transitionMatcher.group('event')
        if (!hasEvent(eventName)) {
            createEvent(eventName, settings.stateMachineName)
        }
        def fromName = aliases[initialStateAlias]
        def toName = aliases[finalStateAlias]
        defineTransition(eventName, fromName, toName)
        log "Potential transition: (${fromName} --> ${toName}: '${eventName}'"
    }
}

private static String indentation() { '    ' }

private String asMermaid(List<Map> transitions) {
    String mermaid = "stateDiagram-v2\n"
//    Map<String, Integer>stateOccurences = getStateOccurences(transitions, [:])

    Map<String, String> stateMap = [:]
    //@TODO should sort the states by the number of transitions that apply in descending order
    def stateNo = 1
    enumerateStates().each {
        def name = cleanupName(it.displayName.toString())
        def stateDef = "s${stateNo++}"
        mermaid += indentation() + "state \"${name}\" as ${stateDef}\n"
        stateMap[name] = stateDef as String
//        stateOccurences[name] = 0
    }
    log "State map: ${stateMap}"
    transitions.each {
        mermaid += asMermaid(it, stateMap)
    }
    mermaid
}

private Map<String, Integer> getStateOccurences(List<Map> transitions, Map<String, Integer> stateOccurences) {
    transitions.each {
        def from = it.from
        def to = it
        if (stateOccurences.containsKey(from)) {
            stateOccurences[key] = stateOccurences[key] + 1
        }
        if (stateNames.containsKey(to)) {
            stateOccurences[key] = stateOccurences[key] + 1
        }
    }
    stateOccurences
}

private static String asMermaid(Map<String, String> transition, Map stateMap) {
    indentation() + "${cleanupStateName(transition.from, stateMap)}-->${cleanupStateName(transition.to, stateMap)}:${cleanupName(transition.event)}\n"
}

static String cleanupName(String name) {
    String[] parts = name.split(':')
    (parts.size() == 1) ? name : parts[1]
}

private static String cleanupStateName(String s, Map stateMap) {
    def name = cleanupName(s)
    stateMap[name]
}

def getStateContainer() {
    getChildDevices().find { it.getName() == getStateContainerName() }
}

def getEventContainer() {
    getChildDevices().find { it.getName() == getEventContainerName() }
}

boolean developingUpgrade() { true }


private GString getEventContainerName() {
    "${settings.stateMachineName}:Events"
}

private GString getStateContainerName() {
    "${settings.stateMachineName}:States"
}

private boolean hasEvent(String eventName) {
    def eventContainer = getEventContainer()
    if (null == eventContainer) {
        false
    } else {
        def nen = makeEventName(eventName)
        eventContainer.getChildDevice(nen)
    }
}

private GString makeEventName(String eventName) {
    "Event;${settings.stateMachineName};${eventName}"
}

private void forceStateActive(newState) {
    log.info "Forcing state ${newState}"
    def currentState = atomicState.currentState
    if (newState != currentState) {
        changeCurrentState(currentState, newState)
    }
}