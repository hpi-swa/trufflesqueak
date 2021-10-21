Class {
	#name : #AWFYParseException,
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
AWFYParseException class >> with: aMessageString at: offset line: line column: column [

    ^ self new initializeWith: aMessageString
                           at: offset
                         line: line
                       column: column

]

{ #category : #'as yet unclassified' }
AWFYParseException >> asString [
 ^ msg + ':', line, ':', column 
]

{ #category : #'as yet unclassified' }
AWFYParseException >> column [
 ^ column 
]

{ #category : #'as yet unclassified' }
AWFYParseException >> initializeWith: message at: anOffset line: aLine column: aColumn [

    msg    := message.
    offset := anOffset.
    line   := aLine.
    column := aColumn.

]

{ #category : #'as yet unclassified' }
AWFYParseException >> line [
 ^ line 
]

{ #category : #'as yet unclassified' }
AWFYParseException >> message [
 ^ msg 
]

{ #category : #'as yet unclassified' }
AWFYParseException >> offset [
 ^ offset 
]
