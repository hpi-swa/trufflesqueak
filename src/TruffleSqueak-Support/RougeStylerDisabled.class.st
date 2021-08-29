Class {
	#name : #RougeStylerDisabled,
	#superclass : #RougeStyler,
	#instVars : [
		'stStyler'
	],
	#category : #'TruffleSqueak-Support'
}

{ #category : #'initialize-release' }
RougeStylerDisabled >> initialize [
	super initialize.
	stStyler := SHTextStylerST80 new
]

{ #category : #'public-api' }
RougeStylerDisabled >> style: aText language: aLanguageID [
	aLanguageID = Polyglot languageIdSmalltalk
		ifTrue: [ stStyler styledTextFor: aText ]
]

{ #category : #'public-api' }
RougeStylerDisabled >> style: aText language: aLanguageID ifFail: aBlock [
	aBlock value
]
