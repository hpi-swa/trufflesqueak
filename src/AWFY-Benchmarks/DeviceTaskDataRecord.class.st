Class {
	#name : #DeviceTaskDataRecord,
	#superclass : #RBObject,
	#instVars : [
		'pending'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
DeviceTaskDataRecord class >> create [

    ^ super new create

]

{ #category : #'as yet unclassified' }
DeviceTaskDataRecord >> create [
 pending := RBObject NoWork 
]

{ #category : #'as yet unclassified' }
DeviceTaskDataRecord >> pending [
 ^ pending 
]

{ #category : #'as yet unclassified' }
DeviceTaskDataRecord >> pending: packet [
 pending := packet 
]
