Class {
	#name : #Packet,
	#superclass : #RBObject,
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
Packet class >> create: link identity: identity kind: kind [
 
    ^ super new
        link: link
        identity: identity
        kind: kind

]

{ #category : #'as yet unclassified' }
Packet >> asString [

      ^ 'Packet(', 
          link asString, ', ',
          identity asString, ', ',
          kind asString, ', ',
          datum asString, ', ',
          data asString,
        ')'

]

{ #category : #'as yet unclassified' }
Packet >> data [
 ^ data 
]

{ #category : #'as yet unclassified' }
Packet >> datum [
 ^ datum 
]

{ #category : #'as yet unclassified' }
Packet >> datum: someData [
 datum := someData 
]

{ #category : #'as yet unclassified' }
Packet >> identity [
 ^ identity 
]

{ #category : #'as yet unclassified' }
Packet >> identity: anIdentity [
 identity := anIdentity 
]

{ #category : #'as yet unclassified' }
Packet >> kind [
 ^ kind 
]

{ #category : #'as yet unclassified' }
Packet >> link [
 ^ link 
]

{ #category : #'as yet unclassified' }
Packet >> link: aWorkQueue [
 link := aWorkQueue 
]

{ #category : #'as yet unclassified' }
Packet >> link: aLink identity: anIdentity kind: aKind [

    link := aLink.
    kind := aKind.
    identity := anIdentity. 
    datum := 1.
    data  := Array new: 4 withAll: 0

]
