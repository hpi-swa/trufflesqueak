Class {
	#name : #IdleTaskDataRecord,
	#superclass : #RBObject,
	#instVars : [
		'control',
		'count'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
IdleTaskDataRecord class >> create [
 ^ super new create 
]

{ #category : #'as yet unclassified' }
IdleTaskDataRecord >> control [
 ^ control 
]

{ #category : #'as yet unclassified' }
IdleTaskDataRecord >> control: aNumber [

    control := aNumber

]

{ #category : #'as yet unclassified' }
IdleTaskDataRecord >> count [
 ^ count 
]

{ #category : #'as yet unclassified' }
IdleTaskDataRecord >> count: aCount [

    count := aCount

]

{ #category : #'as yet unclassified' }
IdleTaskDataRecord >> create [

    control := 1.
    count := 10000

]
