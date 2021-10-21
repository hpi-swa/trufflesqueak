Class {
	#name : #AWFYPacket,
	#superclass : #AWFYRBObject,
	#instVars : [
		'link',
		'identity',
		'kind',
		'datum',
		'data'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYPacket class >> create: link identity: identity kind: kind [
 
    ^ super new
        link: link
        identity: identity
        kind: kind

]

{ #category : #'as yet unclassified' }
AWFYPacket >> asString [

      ^ 'Packet(', 
          link asString, ', ',
          identity asString, ', ',
          kind asString, ', ',
          datum asString, ', ',
          data asString,
        ')'

]

{ #category : #'as yet unclassified' }
AWFYPacket >> data [
 ^ data 
]

{ #category : #'as yet unclassified' }
AWFYPacket >> datum [
 ^ datum 
]

{ #category : #'as yet unclassified' }
AWFYPacket >> datum: someData [
 datum := someData 
]

{ #category : #'as yet unclassified' }
AWFYPacket >> identity [
 ^ identity 
]

{ #category : #'as yet unclassified' }
AWFYPacket >> identity: anIdentity [
 identity := anIdentity 
]

{ #category : #'as yet unclassified' }
AWFYPacket >> kind [
 ^ kind 
]

{ #category : #'as yet unclassified' }
AWFYPacket >> link [
 ^ link 
]

{ #category : #'as yet unclassified' }
AWFYPacket >> link: aWorkQueue [
 link := aWorkQueue 
]

{ #category : #'as yet unclassified' }
AWFYPacket >> link: aLink identity: anIdentity kind: aKind [

    link := aLink.
    kind := aKind.
    identity := anIdentity. 
    datum := 1.
    data  := Array new: 4 withAll: 0

]
