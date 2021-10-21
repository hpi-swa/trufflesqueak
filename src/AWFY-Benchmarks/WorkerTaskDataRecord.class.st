Class {
	#name : #WorkerTaskDataRecord,
	#superclass : #RBObject,
	#instVars : [
		'destination',
		'count'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
WorkerTaskDataRecord class >> create [
 ^ super new create 
]

{ #category : #'as yet unclassified' }
WorkerTaskDataRecord >> count [
 ^ count 
]

{ #category : #'as yet unclassified' }
WorkerTaskDataRecord >> count: aCount [
count := aCount 
]

{ #category : #'as yet unclassified' }
WorkerTaskDataRecord >> create [

      destination := RBObject HandlerA.
      count := 0

]

{ #category : #'as yet unclassified' }
WorkerTaskDataRecord >> destination [
 ^ destination 
]

{ #category : #'as yet unclassified' }
WorkerTaskDataRecord >> destination: aHandler [
 destination := aHandler 
]
