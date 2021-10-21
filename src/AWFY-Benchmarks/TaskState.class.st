Class {
	#name : #TaskState,
	#superclass : #RBObject,
	#instVars : [
		'packetPending',
		'taskWaiting',
		'taskHolding'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
TaskState class >> packetPending [
 ^ super new packetPending 
]

{ #category : #'as yet unclassified' }
TaskState class >> running [
 ^ super new running 
]

{ #category : #'as yet unclassified' }
TaskState class >> waiting [
 ^ super new waiting 
]

{ #category : #'as yet unclassified' }
TaskState class >> waitingWithPacket [
 ^ super new waitingWithPacket 
]

{ #category : #'as yet unclassified' }
TaskState >> isPacketPending [
 ^ packetPending 
]

{ #category : #'as yet unclassified' }
TaskState >> isRunning [
 ^ packetPending not and: [taskWaiting not and: [taskHolding not]] 
]

{ #category : #'as yet unclassified' }
TaskState >> isTaskHolding [
 ^ taskHolding 
]

{ #category : #'as yet unclassified' }
TaskState >> isTaskHoldingOrWaiting [
 ^ taskHolding or: [packetPending not and: [taskWaiting]] 
]

{ #category : #'as yet unclassified' }
TaskState >> isTaskWaiting [
 ^ taskWaiting 
]

{ #category : #'as yet unclassified' }
TaskState >> isWaiting [
 ^ packetPending not and: [taskWaiting and: [taskHolding not]] 
]

{ #category : #'as yet unclassified' }
TaskState >> isWaitingWithPacket [
 ^ packetPending and: [taskWaiting and: [taskHolding not]] 
]

{ #category : #'as yet unclassified' }
TaskState >> packetPending [

      packetPending := true.
      taskWaiting := false.
      taskHolding := false

]

{ #category : #'as yet unclassified' }
TaskState >> packetPending: aBoolean [
 packetPending := aBoolean 
]

{ #category : #'as yet unclassified' }
TaskState >> running [

      packetPending := taskWaiting := taskHolding := false

]

{ #category : #'as yet unclassified' }
TaskState >> taskHolding: aBoolean [
 taskHolding   := aBoolean 
]

{ #category : #'as yet unclassified' }
TaskState >> taskWaiting: aBoolean [
 taskWaiting   := aBoolean 
]

{ #category : #'as yet unclassified' }
TaskState >> waiting [

      packetPending := taskHolding := false.
      taskWaiting := true

]

{ #category : #'as yet unclassified' }
TaskState >> waitingWithPacket [

      taskHolding := false.
      taskWaiting := packetPending := true

]
