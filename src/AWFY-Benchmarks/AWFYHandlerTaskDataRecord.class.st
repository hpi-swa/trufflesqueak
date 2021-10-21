Class {
	#name : #AWFYHandlerTaskDataRecord,
	#superclass : #AWFYRBObject,
	#instVars : [
		'workIn',
		'deviceIn'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYHandlerTaskDataRecord class >> create [
 ^ super new create 
]

{ #category : #'as yet unclassified' }
AWFYHandlerTaskDataRecord >> asString [

      ^ 'HandlerTaskDataRecord(', workIn asString, ', ', deviceIn asString, ')'

]

{ #category : #'as yet unclassified' }
AWFYHandlerTaskDataRecord >> create [

    workIn := deviceIn := AWFYRBObject NoWork

]

{ #category : #'as yet unclassified' }
AWFYHandlerTaskDataRecord >> deviceIn [
 ^ deviceIn 
]

{ #category : #'as yet unclassified' }
AWFYHandlerTaskDataRecord >> deviceIn: aPacket [
 deviceIn := aPacket 
]

{ #category : #'as yet unclassified' }
AWFYHandlerTaskDataRecord >> deviceInAdd: packet [

      deviceIn := self append: packet head: deviceIn

]

{ #category : #'as yet unclassified' }
AWFYHandlerTaskDataRecord >> workIn [
 ^ workIn 
]

{ #category : #'as yet unclassified' }
AWFYHandlerTaskDataRecord >> workIn: aWorkQueue [
 workIn := aWorkQueue 
]

{ #category : #'as yet unclassified' }
AWFYHandlerTaskDataRecord >> workInAdd: packet [

      workIn := self append: packet head: workIn

]
