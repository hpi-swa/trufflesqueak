Class {
	#name : #LanguageInfoStub,
	#superclass : #Object,
	#classVars : [
		'Instance'
	],
	#category : 'TruffleSqueak-Support'
}

{ #category : #accessing }
LanguageInfoStub class >> instance [
	"Answer the receiver's instance"
	^ Instance ifNil: [ Instance := self new ]
]

{ #category : #accessing }
LanguageInfoStub >> getDefaultMimeType [
	^ '.st'
]

{ #category : #accessing }
LanguageInfoStub >> getId [
	^ Polyglot languageIdSmalltalk
]

{ #category : #accessing }
LanguageInfoStub >> getMimeTypes [
	^ {self getDefaultMimeType}
]

{ #category : #accessing }
LanguageInfoStub >> getName [
	^ 'Squeak/Smalltalk'
]

{ #category : #accessing }
LanguageInfoStub >> getVersion [
	^ '1.0'
]

{ #category : #accessing }
LanguageInfoStub >> isInteractive [
	^ true
]

{ #category : #accessing }
LanguageInfoStub >> isInternal [
	^ false
]
