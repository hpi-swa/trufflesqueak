Class {
	#name : #AWFYTaskControlBlock,
	#superclass : #AWFYTaskState,
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
AWFYTaskControlBlock class >> link: link create: identity priority: priority initialWorkQueue: initialWorkQueue initialState: initialState function: aBlock privateData: privateData [

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
AWFYTaskControlBlock >> addInput: packet checkPriority: oldTask [

    AWFYRBObject NoWork == input
      ifTrue: [
        input := packet.
        self packetPending: true.
        priority > oldTask priority ifTrue: [ ^ self ] ]
      ifFalse: [
        input := self append: packet head: input ].
    ^ oldTask

]

{ #category : #'as yet unclassified' }
AWFYTaskControlBlock >> identity [
 ^ identity 
]

{ #category : #'as yet unclassified' }
AWFYTaskControlBlock >> link [
 ^ link 
]

{ #category : #'as yet unclassified' }
AWFYTaskControlBlock >> link: aLink identity: anIdentity priority: aPriority initialWorkQueue: anInitialWorkQueue initialState: anInitialState function: aBlock privateData: aPrivateData [

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
AWFYTaskControlBlock >> priority [
 ^ priority 
]

{ #category : #'as yet unclassified' }
AWFYTaskControlBlock >> runTask [

    | message |
    self isWaitingWithPacket
      ifTrue: [
        message := input.
        input := message link.
        AWFYRBObject NoWork == input
          ifTrue: [self running]
          ifFalse: [self packetPending]]
      ifFalse: [message := AWFYRBObject NoWork].
    ^ function value: message value: handle

]
