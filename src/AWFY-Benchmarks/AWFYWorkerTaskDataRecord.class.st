Class {
	#name : #AWFYWorkerTaskDataRecord,
	#superclass : #AWFYRBObject,
	#instVars : [
		'destination',
		'count'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYWorkerTaskDataRecord class >> create [
 ^ super new create 
]

{ #category : #'as yet unclassified' }
AWFYWorkerTaskDataRecord >> count [
 ^ count 
]

{ #category : #'as yet unclassified' }
AWFYWorkerTaskDataRecord >> count: aCount [
count := aCount 
]

{ #category : #'as yet unclassified' }
AWFYWorkerTaskDataRecord >> create [

      destination := AWFYRBObject HandlerA.
      count := 0

]

{ #category : #'as yet unclassified' }
AWFYWorkerTaskDataRecord >> destination [
 ^ destination 
]

{ #category : #'as yet unclassified' }
AWFYWorkerTaskDataRecord >> destination: aHandler [
 destination := aHandler 
]
