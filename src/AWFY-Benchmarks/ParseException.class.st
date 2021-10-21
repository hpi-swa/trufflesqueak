Class {
	#name : #ParseException,
	#superclass : #Object,
	#instVars : [
		'offset',
		'line',
		'column',
		'msg'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
ParseException class >> with: aMessageString at: offset line: line column: column [

    ^ self new initializeWith: aMessageString
                           at: offset
                         line: line
                       column: column

]

{ #category : #'as yet unclassified' }
ParseException >> asString [
 ^ msg + ':', line, ':', column 
]

{ #category : #'as yet unclassified' }
ParseException >> column [
 ^ column 
]

{ #category : #'as yet unclassified' }
ParseException >> initializeWith: message at: anOffset line: aLine column: aColumn [

    msg    := message.
    offset := anOffset.
    line   := aLine.
    column := aColumn.

]

{ #category : #'as yet unclassified' }
ParseException >> line [
 ^ line 
]

{ #category : #'as yet unclassified' }
ParseException >> message [
 ^ msg 
]

{ #category : #'as yet unclassified' }
ParseException >> offset [
 ^ offset 
]
