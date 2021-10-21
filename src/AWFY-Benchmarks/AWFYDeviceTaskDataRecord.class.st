Class {
	#name : #AWFYDeviceTaskDataRecord,
	#superclass : #AWFYRBObject,
	#instVars : [
		'pending'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYDeviceTaskDataRecord class >> create [

    ^ super new create

]

{ #category : #'as yet unclassified' }
AWFYDeviceTaskDataRecord >> create [
 pending := AWFYRBObject NoWork 
]

{ #category : #'as yet unclassified' }
AWFYDeviceTaskDataRecord >> pending [
 ^ pending 
]

{ #category : #'as yet unclassified' }
AWFYDeviceTaskDataRecord >> pending: packet [
 pending := packet 
]
