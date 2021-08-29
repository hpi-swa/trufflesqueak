Class {
	#name : #PELanguageInfoPolyglot,
	#superclass : #PELanguageInfo,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #polyglot }
PELanguageInfoPolyglot class >> codeForAssignmentOf: value to: name [

	self subclassResponsibility
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> codeForComment: string [

	self subclassResponsibility
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> codeForEscapedString: string [

	self subclassResponsibility
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> codeForPolyglotEvaluate: code in: language [

	self subclassResponsibility
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> codeForPolyglotEvaluateFile: path in: language [

	self subclassResponsibility
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> codeForPolyglotExportOf: value to: name [

	self subclassResponsibility
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> codeForPolyglotHeader [

	self subclassResponsibility
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> codeForPolyglotImport: aString [

	self subclassResponsibility
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> codeForPredeclaringVariables: aCollection [

	"This is mostly here for Smalltalk, where variables need to be declared at the top of the method before being used"
	^ ''
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> codeForStatementSeparator [

	self subclassResponsibility
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> customCodeAfterCodeBoxExports [

	^ ''
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> customCodeBeforeCodeBoxContent [

	^ ''
]

{ #category : #private }
PELanguageInfoPolyglot class >> generateOneLineCommentsFor: aString startingWith: anotherString [

	| result |
	result := ''.
	aString lineIndicesDo: [:start :end :endWithDelimiter | result := result, anotherString, (aString copyFrom: start to: endWithDelimiter)].
	^ result
]

{ #category : #accessing }
PELanguageInfoPolyglot class >> hasPolyglotSupport [

	"true when we are not in TruffleSqueak, otherwise only true if the language is installed"
	| polyglot |
	polyglot := Smalltalk classNamed: #Polyglot.
	^ polyglot isNil or: [polyglot isPolyglotEvalAllowed not] or: [polyglot isPermitted: self polyglotId]
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> polyglotId [

	self subclassResponsibility
]

{ #category : #polyglot }
PELanguageInfoPolyglot class >> unescapedCommentContent: aString [

	self subclassResponsibility
]

{ #category : #private }
PELanguageInfoPolyglot class >> unescapedCommentContent: aString eachCommentLineStartingWith: anotherString [

	| endOfComment stop content |
	content := aString.
	stop := false.
	aString lineIndicesDo: [:start :end :endWithLineEnding | | line |
		stop ifFalse: [
			line := aString copyFrom: start to: end.
			"We dont't check for the comment begin marker within the first line since we know we are already in a comment"
			start = 1
				ifTrue: [content := line]
			 	ifFalse: [
					(line beginsWith: anotherString)
						ifTrue: [
							endOfComment := end.
							content := content, (line copyFrom: anotherString size + 1 to: line size)]
						ifFalse: [stop := true]]]].
	
	^ content
]
