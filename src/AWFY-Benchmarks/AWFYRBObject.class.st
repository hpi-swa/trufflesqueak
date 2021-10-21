Class {
	#name : #AWFYRBObject,
	#superclass : #Object,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYRBObject class >> DeviceA [
 ^ 5 
]

{ #category : #'as yet unclassified' }
AWFYRBObject class >> DeviceB [
 ^ 6 
]

{ #category : #'as yet unclassified' }
AWFYRBObject class >> DevicePacketKind [
 ^ 1 
]

{ #category : #'as yet unclassified' }
AWFYRBObject class >> HandlerA [
 ^ 3 
]

{ #category : #'as yet unclassified' }
AWFYRBObject class >> HandlerB [
 ^ 4 
]

{ #category : #'as yet unclassified' }
AWFYRBObject class >> Idler [
 ^ 1 
]

{ #category : #'as yet unclassified' }
AWFYRBObject class >> NoTask [
 ^ nil 
]

{ #category : #'as yet unclassified' }
AWFYRBObject class >> NoWork [
 ^ nil 
]

{ #category : #'as yet unclassified' }
AWFYRBObject class >> WorkPacketKind [
 ^ 2 
]

{ #category : #'as yet unclassified' }
AWFYRBObject class >> Worker [
 ^ 2 
]

{ #category : #'as yet unclassified' }
AWFYRBObject >> append: packet head: queueHead [

    | mouse link |
    packet link: AWFYRBObject NoWork.
    AWFYRBObject NoWork == queueHead ifTrue: [ ^ packet ].
    mouse := queueHead.
    [AWFYRBObject NoWork == (link := mouse link)]
            whileFalse: [mouse := link].
    mouse link: packet.
    ^ queueHead

]
