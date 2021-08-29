Class {
	#name : #PELanguageInfoR,
	#superclass : #PELanguageInfoPolyglot,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #polyglot }
PELanguageInfoR class >> codeForAssignmentOf: value to: name [

	^ name, ' <- ', value
]

{ #category : #polyglot }
PELanguageInfoR class >> codeForComment: string [

	^ self generateOneLineCommentsFor: string startingWith: '# '
]

{ #category : #polyglot }
PELanguageInfoR class >> codeForEscapedString: string [

	^ '"', (string copyReplaceAll: '"' with: '\"'),  '"'
]

{ #category : #polyglot }
PELanguageInfoR class >> codeForPolyglotEvaluate: code in: language [

	^  'eval.polyglot(', (self codeForEscapedString: language), ', ', (self codeForEscapedString: code), ')'
]

{ #category : #polyglot }
PELanguageInfoR class >> codeForPolyglotEvaluateFile: path in: language [

	^  'eval.polyglot(', (self codeForEscapedString: language), ', path=', (self codeForEscapedString: path), ')'
]

{ #category : #polyglot }
PELanguageInfoR class >> codeForPolyglotExportOf: value to: name [

	^ 'export(', (self codeForEscapedString: name), ', ', value, ')'
]

{ #category : #polyglot }
PELanguageInfoR class >> codeForPolyglotHeader [

	^ ''
]

{ #category : #polyglot }
PELanguageInfoR class >> codeForPolyglotImport: name [

	^ 'import(', (self codeForEscapedString: name), ')'
]

{ #category : #polyglot }
PELanguageInfoR class >> codeForStatementSeparator [

	^ ''
]

{ #category : #accessing }
PELanguageInfoR class >> color [

	^ Color fromString:  '#81D4FA'
]

{ #category : #accessing }
PELanguageInfoR class >> defaultLineEndings [

	^ String lf
]

{ #category : #accessing }
PELanguageInfoR class >> extensions [

	^ #(r)
]

{ #category : #accessing }
PELanguageInfoR class >> id [

	^ self polyglotId
]

{ #category : #accessing }
PELanguageInfoR class >> languageName [

	^ 'R'
]

{ #category : #polyglot }
PELanguageInfoR class >> polyglotId [

	^ Smalltalk globals at: #Polyglot ifPresent: #languageIdR ifAbsent: [#R]
]

{ #category : #accessing }
PELanguageInfoR class >> rougeLexerName [

	^ 'r'
]

{ #category : #polyglot }
PELanguageInfoR class >> unescapedCommentContent: aString [

	^ self unescapedCommentContent: aString eachCommentLineStartingWith: '# '
]
