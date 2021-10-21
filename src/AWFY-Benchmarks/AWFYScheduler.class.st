Class {
	#name : #AWFYScheduler,
	#superclass : #AWFYRBObject,
	#instVars : [
		'taskList',
		'currentTask',
		'currentTaskIdentity',
		'taskTable',
		'tracing',
		'layout',
		'queuePacketCount',
		'holdCount'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYScheduler class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
AWFYScheduler >> createDevice: identity priority: priority work: work state: state [

    | data |
    data := AWFYDeviceTaskDataRecord create.
    self
      createTask: identity
      priority: priority
      work: work
      state: state
      function: 
        [:thework :word |
        | thedata functionWork | 
        thedata := word.
        functionWork := thework.
        AWFYRBObject NoWork == functionWork ifTrue: [
          AWFYRBObject NoWork == (functionWork := thedata pending)
            ifTrue: [ self wait ]
            ifFalse: [
              thedata pending: AWFYRBObject NoWork.
              self queuePacket: functionWork]]
          ifFalse: [
            thedata pending: functionWork.
            self tracing ifTrue: [
              self trace: functionWork datum].
            self holdSelf]]
      data: data

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> createHandler: identity priority: priority work: work state: state [

    | data |
    data := AWFYHandlerTaskDataRecord create.
    self createTask: identity
         priority:   priority
         work:       work
         state:      state
         function:   [:thework :word | 
           | thedata workPacket |
           thedata := word.
           AWFYRBObject NoWork == thework ifFalse: [
             AWFYRBObject WorkPacketKind == thework kind 
               ifTrue:  [ thedata workInAdd: thework ]
               ifFalse: [ thedata deviceInAdd: thework ]].

           AWFYRBObject NoWork == (workPacket := thedata workIn)
             ifTrue: [ self wait ]
             ifFalse: [
               | count |
               count := workPacket datum.
               count > 4
                 ifTrue: [
                   thedata workIn: workPacket link.
                   self queuePacket: workPacket]
                 ifFalse: [
                   | devicePacket |
                   AWFYRBObject NoWork == (devicePacket := thedata deviceIn)
                     ifTrue: [ self wait ]
                     ifFalse: [
                       thedata deviceIn: devicePacket link.
                       devicePacket datum: (workPacket data at: count).
                       workPacket datum: count + 1.
                       self queuePacket: devicePacket]]]]
         data: data

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> createIdler: identity priority: priority work: work state: state [

      | data |
      data := AWFYIdleTaskDataRecord create.
      self createTask: identity
           priority:   priority
           work:       work
           state:      state
           function:  [:thework :word | 
             | thedata |
             thedata := word.
             thedata count: thedata count - 1.
             0 = thedata count
               ifTrue:  [ self holdSelf ]
               ifFalse: [
                 0 = (thedata control bitAnd: 1 )
                 ifTrue: [
                   thedata control: thedata control // 2.
                   self release: AWFYRBObject DeviceA]
                 ifFalse: [
                   thedata control: (thedata control // 2 bitXor: 53256).
                   self release: AWFYRBObject DeviceB]]]
           data: data

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> createPacket: link identity: identity kind: kind [

    ^ AWFYPacket create:   link
             identity: identity
             kind:     kind

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> createTask: identity priority: priority work: work state: state function: aBlock data: data [

    | t |
    t := AWFYTaskControlBlock   link:             taskList
                            create:           identity
                            priority:         priority
                            initialWorkQueue: work
                            initialState:     state
                            function:         aBlock
                            privateData:      data.
    taskList := t.
    taskTable at: identity put: t

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> createWorker: identity priority: priority work: work state: state [

      | data |
      data := AWFYWorkerTaskDataRecord create.
      self createTask: identity
           priority:   priority
           work:       work
           state:      state
           function: [:thework :word |
             | thedata | 
             thedata := word.
             AWFYRBObject NoWork == thework
               ifTrue: [ self wait ]
               ifFalse: [
                 thedata destination: (AWFYRBObject HandlerA = thedata destination
                                       ifTrue:  [AWFYRBObject HandlerB]
                                       ifFalse: [AWFYRBObject HandlerA]).
                 thework identity: thedata destination.
                 thework datum: 1.
                 1 to: 4 do: [:i | 
                   thedata count: thedata count + 1.
                   thedata count > 26 ifTrue: [thedata count: 1].
                   "thework data at: i put: $A asInteger + thedata count - 1]."
                   thework data at: i put: 65 + thedata count - 1].
                 self queuePacket: thework]]
          data: data

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> findTask: identity [

    | t |
    t := taskTable at: identity.
    AWFYRBObject NoTask == t ifTrue: [self error: 'findTask failed'].
    ^ t

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> holdSelf [

    holdCount := holdCount + 1.
    currentTask taskHolding: true.
    ^ currentTask link

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> initialize [

    taskList    := AWFYRBObject NoTask.
    currentTask := AWFYRBObject NoTask.
    currentTaskIdentity := 0.
    taskTable   := Array new: 6 withAll: AWFYRBObject NoTask.
    layout      := 0.
    queuePacketCount := 0.
    holdCount   := 0.

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> queuePacket: packet [

    | t |
    t := self findTask: packet identity.
    AWFYRBObject NoTask == t ifTrue: [ ^ AWFYRBObject NoTask ].
    queuePacketCount := queuePacketCount + 1.
    packet link: AWFYRBObject NoWork.
    packet identity: currentTaskIdentity.
    ^ t addInput: packet checkPriority: currentTask

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> release: identity [

      | t |
      t := self findTask: identity.
      AWFYRBObject NoTask == t ifTrue: [ ^ AWFYRBObject NoTask ].
      t taskHolding: false.
      t priority > currentTask priority
        ifTrue:  [ ^ t ]
        ifFalse: [ ^ currentTask ]

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> schedule [

    currentTask := taskList. 
    [ AWFYRBObject NoTask == currentTask ] whileFalse: [
      currentTask isTaskHoldingOrWaiting
        ifTrue:  [ currentTask := currentTask link ]
        ifFalse: [
          currentTaskIdentity := currentTask identity.
          self tracing ifTrue: [ self trace: currentTaskIdentity ].
          currentTask := currentTask runTask ] ]

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> start [

    | workQ |
    self
            createIdler: AWFYRBObject Idler
            priority: 0
            work: AWFYRBObject NoWork
            state: AWFYTaskState running.
    workQ := self
                            createPacket: AWFYRBObject NoWork
                            identity: AWFYRBObject Worker
                            kind: AWFYRBObject WorkPacketKind.
    workQ := self
                            createPacket: workQ
                            identity: AWFYRBObject Worker
                            kind: AWFYRBObject WorkPacketKind.
    self
            createWorker: AWFYRBObject Worker
            priority: 1000
            work: workQ
            state: AWFYTaskState waitingWithPacket.
    workQ := self
                            createPacket: AWFYRBObject NoWork
                            identity: AWFYRBObject DeviceA
                            kind: AWFYRBObject DevicePacketKind.
    workQ := self
                            createPacket: workQ
                            identity: AWFYRBObject DeviceA
                            kind: AWFYRBObject DevicePacketKind.
    workQ := self
                            createPacket: workQ
                            identity: AWFYRBObject DeviceA
                            kind: AWFYRBObject DevicePacketKind.
    self
            createHandler: AWFYRBObject HandlerA
            priority: 2000
            work: workQ
            state: AWFYTaskState waitingWithPacket.
    workQ := self
                            createPacket: AWFYRBObject NoWork
                            identity: AWFYRBObject DeviceB
                            kind: AWFYRBObject DevicePacketKind.
    workQ := self
                            createPacket: workQ
                            identity: AWFYRBObject DeviceB
                            kind: AWFYRBObject DevicePacketKind.
    workQ := self
                            createPacket: workQ
                            identity: AWFYRBObject DeviceB
                            kind: AWFYRBObject DevicePacketKind.
    self
            createHandler: AWFYRBObject HandlerB
            priority: 3000
            work: workQ
            state: AWFYTaskState waitingWithPacket.
    self
            createDevice: AWFYRBObject DeviceA
            priority: 4000
            work: AWFYRBObject NoWork
            state: AWFYTaskState waiting.
    self
            createDevice: AWFYRBObject DeviceB
            priority: 5000
            work: AWFYRBObject NoWork
            state: AWFYTaskState waiting.

    self schedule.

    ^ queuePacketCount = 23246 and: holdCount = 9297

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> trace: id [

    layout := layout - 1.
    0 >= layout ifTrue: [
      '' println.
      layout := 50 ].
    id print

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> tracing [

    ^ false

]

{ #category : #'as yet unclassified' }
AWFYScheduler >> wait [

    currentTask taskWaiting: true.
    ^currentTask

]
