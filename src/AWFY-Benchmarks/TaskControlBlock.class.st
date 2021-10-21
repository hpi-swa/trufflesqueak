Class {
	#name : #TaskControlBlock,
	#superclass : #TaskState,
	#instVars : [
		'link',
		'identity',
		'priority',
		'input',
		'function',
		'handle'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
TaskControlBlock class >> link: link create: identity priority: priority initialWorkQueue: initialWorkQueue initialState: initialState function: aBlock privateData: privateData [

      ^super new
              link: link
              identity: identity
              priority: priority
              initialWorkQueue: initialWorkQueue
              initialState: initialState
              function: aBlock
              privateData: privateData

]

{ #category : #'as yet unclassified' }
TaskControlBlock >> addInput: packet checkPriority: oldTask [

    RBObject NoWork == input
      ifTrue: [
        input := packet.
        self packetPending: true.
        priority > oldTask priority ifTrue: [ ^ self ] ]
      ifFalse: [
        input := self append: packet head: input ].
    ^ oldTask

]

{ #category : #'as yet unclassified' }
TaskControlBlock >> identity [
 ^ identity 
]

{ #category : #'as yet unclassified' }
TaskControlBlock >> link [
 ^ link 
]

{ #category : #'as yet unclassified' }
TaskControlBlock >> link: aLink identity: anIdentity priority: aPriority initialWorkQueue: anInitialWorkQueue initialState: anInitialState function: aBlock privateData: aPrivateData [

    link     := aLink.
    identity := anIdentity.
    function := aBlock.
    priority := aPriority.
    input    := anInitialWorkQueue.
    handle   := aPrivateData.
    self packetPending: anInitialState isPacketPending.
    self taskWaiting: anInitialState isTaskWaiting.
    self taskHolding: anInitialState isTaskHolding.

]

{ #category : #'as yet unclassified' }
TaskControlBlock >> priority [
 ^ priority 
]

{ #category : #'as yet unclassified' }
TaskControlBlock >> runTask [

    | message |
    self isWaitingWithPacket
      ifTrue: [
        message := input.
        input := message link.
        RBObject NoWork == input
          ifTrue: [self running]
          ifFalse: [self packetPending]]
      ifFalse: [message := RBObject NoWork].
    ^ function value: message value: handle

]
