Class {
	#name : #PELanguageInfoSmalltalk,
	#superclass : #PELanguageInfoPolyglot,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> codeForAssignmentOf: value to: name [

	^ name, ' := ', value
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> codeForComment: string [

	^ '"', (string copyReplaceAll: '"' with: '""'), '"'
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> codeForEscapedString: string [

	^ '''', (string copyReplaceAll: '''' with: ''''''),  ''''
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> codeForPolyglotEvaluate: code in: language [

	^ 'Polyglot eval: ', (self codeForEscapedString: language), ' string: ', (self codeForEscapedString: code)
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> codeForPolyglotEvaluateFile: path in: language [

	^ 'Polyglot eval: ', (self codeForEscapedString: language), ' file: ', (self codeForEscapedString: path)
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> codeForPolyglotExportOf: value to: name [

	^ 'Polyglot export: ', (self codeForEscapedString: name), ' value: (', value, ')'
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> codeForPolyglotHeader [

	^ ''
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> codeForPolyglotImport: name [

	^ 'Polyglot import: ', (self codeForEscapedString: name)
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> codeForPredeclaringVariables: aCollection [

	aCollection ifEmpty: [^ ''].
	^ '| ', (aCollection joinSeparatedBy: ' '), ' |'
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> codeForStatementSeparator [

	^ '.'
]

{ #category : #accessing }
PELanguageInfoSmalltalk class >> color [

	^ Color fromString:  '#9CCC65'
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> customCodeAfterCodeBoxExports [

	"Wrap the code in a block so that declaration of temporaries does not appear in the middle of the file."
	^ '] value.'
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> customCodeBeforeCodeBoxContent [

	"Wrap the code in a block so that declaration of temporaries does not appear in the middle of the file."
	^ '['
]

{ #category : #accessing }
PELanguageInfoSmalltalk class >> defaultLineEndings [

	^ String cr
]

{ #category : #accessing }
PELanguageInfoSmalltalk class >> extensions [

	^ #(st)
]

{ #category : #accessing }
PELanguageInfoSmalltalk class >> id [

	^ self polyglotId
]

{ #category : #testing }
PELanguageInfoSmalltalk class >> isSmalltalk [

	^ true
]

{ #category : #accessing }
PELanguageInfoSmalltalk class >> languageName [

	^ 'Smalltalk'
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> polyglotId [

	^ Smalltalk globals at: #Polyglot ifPresent: #languageIdSmalltalk ifAbsent: [#smalltalk]
]

{ #category : #accessing }
PELanguageInfoSmalltalk class >> rougeLexerName [

	^ 'smalltalk'
]

{ #category : #polyglot }
PELanguageInfoSmalltalk class >> unescapedCommentContent: aString [

	| found start endOfComment |
	"Search for the first occurrence of a double quote that is not followed by another double quote."
	found := false.
	start := 1.
	[found] whileFalse: [
		endOfComment := aString indexOf: $" startingAt: start.
		(endOfComment = 0 or: [endOfComment = aString size] or: [(aString at: endOfComment + 1) ~= $"])
			ifTrue: [found := true]
			ifFalse: [start := endOfComment + 2]].
	
	"Copy string up until the double quote (if found) and replace all double double quotes by a single double quote"
	endOfComment = 0 
		ifTrue: [endOfComment := aString size]
		ifFalse: [endOfComment := endOfComment - 1].
	^ (aString copyFrom: 1 to: endOfComment) copyReplaceAll: '""' with: '"'
]
