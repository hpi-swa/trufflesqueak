Class {
	#name : #AWFYTaskState,
	#superclass : #AWFYRBObject,
	#instVars : [
		'packetPending',
		'taskWaiting',
		'taskHolding'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYTaskState class >> packetPending [
 ^ super new packetPending 
]

{ #category : #'as yet unclassified' }
AWFYTaskState class >> running [
 ^ super new running 
]

{ #category : #'as yet unclassified' }
AWFYTaskState class >> waiting [
 ^ super new waiting 
]

{ #category : #'as yet unclassified' }
AWFYTaskState class >> waitingWithPacket [
 ^ super new waitingWithPacket 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> isPacketPending [
 ^ packetPending 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> isRunning [
 ^ packetPending not and: [taskWaiting not and: [taskHolding not]] 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> isTaskHolding [
 ^ taskHolding 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> isTaskHoldingOrWaiting [
 ^ taskHolding or: [packetPending not and: [taskWaiting]] 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> isTaskWaiting [
 ^ taskWaiting 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> isWaiting [
 ^ packetPending not and: [taskWaiting and: [taskHolding not]] 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> isWaitingWithPacket [
 ^ packetPending and: [taskWaiting and: [taskHolding not]] 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> packetPending [

      packetPending := true.
      taskWaiting := false.
      taskHolding := false

]

{ #category : #'as yet unclassified' }
AWFYTaskState >> packetPending: aBoolean [
 packetPending := aBoolean 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> running [

      packetPending := taskWaiting := taskHolding := false

]

{ #category : #'as yet unclassified' }
AWFYTaskState >> taskHolding: aBoolean [
 taskHolding   := aBoolean 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> taskWaiting: aBoolean [
 taskWaiting   := aBoolean 
]

{ #category : #'as yet unclassified' }
AWFYTaskState >> waiting [

      packetPending := taskHolding := false.
      taskWaiting := true

]

{ #category : #'as yet unclassified' }
AWFYTaskState >> waitingWithPacket [

      taskHolding := false.
      taskWaiting := packetPending := true

]
