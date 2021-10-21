Class {
	#name : #AWFYIdleTaskDataRecord,
	#superclass : #AWFYRBObject,
	#instVars : [
		'control',
		'count'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYIdleTaskDataRecord class >> create [
 ^ super new create 
]

{ #category : #'as yet unclassified' }
AWFYIdleTaskDataRecord >> control [
 ^ control 
]

{ #category : #'as yet unclassified' }
AWFYIdleTaskDataRecord >> control: aNumber [

    control := aNumber

]

{ #category : #'as yet unclassified' }
AWFYIdleTaskDataRecord >> count [
 ^ count 
]

{ #category : #'as yet unclassified' }
AWFYIdleTaskDataRecord >> count: aCount [

    count := aCount

]

{ #category : #'as yet unclassified' }
AWFYIdleTaskDataRecord >> create [

    control := 1.
    count := 10000

]
