Class {
	#name : #RougeStyler,
	#superclass : #Object,
	#classInstVars : [
		'Default'
	],
	#category : #'TruffleSqueak-Support'
}

{ #category : #accessing }
RougeStyler class >> default [
	"Default := nil"
	^ Default ifNil: [ Default := ((Polyglot isPermitted: #ruby)
		ifTrue: [ RougeStylerEnabled ]
		ifFalse: [ RougeStylerDisabled ])
			basicNew initialize ]
]

{ #category : #'instance creation' }
RougeStyler class >> new [
	self error: 'Use #default instead.'
]

{ #category : #preferences }
RougeStyler class >> useRougeStyler [

	<preference: 'Use rouge for syntax highlighting'
		category: 'Polyglot'
		description: 'If true, use Ruby''s rouge gem for syntax highlighting across languages.'
		type: #Boolean>
	^ Default class == RougeStylerEnabled
]

{ #category : #preferences }
RougeStyler class >> useRougeStyler: aBoolean [

	self useRougeStyler == aBoolean ifFalse: [
		Default := ((aBoolean and: [ Polyglot isPermitted: #ruby ])
			ifTrue: [ RougeStylerEnabled ]
			ifFalse: [ RougeStylerDisabled ]) basicNew initialize ]
]

{ #category : #'public-api' }
RougeStyler >> style: aText language: aLanguageID [
	self subclassResponsibility
]

{ #category : #'public-api' }
RougeStyler >> style: aText language: aLanguageID ifFail: aBlock [
	self subclassResponsibility
]
