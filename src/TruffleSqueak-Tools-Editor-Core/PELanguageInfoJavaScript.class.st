Class {
	#name : #PELanguageInfoJavaScript,
	#superclass : #PELanguageInfoPolyglot,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #polyglot }
PELanguageInfoJavaScript class >> codeForAssignmentOf: value to: name [

	"Use var instead of let until running in an inner context is possible. Otherwise we can only execute the code once, because defining the same variable multiple times using let isn't allowed (it is with var)."
	^ 'var ', name, ' = ', value
]

{ #category : #polyglot }
PELanguageInfoJavaScript class >> codeForComment: string [

	^ self generateOneLineCommentsFor: string startingWith: '// '
]

{ #category : #polyglot }
PELanguageInfoJavaScript class >> codeForEscapedString: string [

	string lineCount > 1 ifTrue: [UIManager inform: 'Cannot properly escape multiline string for JavaScript'].
	^ '"', ((string copyReplaceAll: '\' with: '\\') copyReplaceAll: '"' with: '\"'),  '"'
]

{ #category : #polyglot }
PELanguageInfoJavaScript class >> codeForPolyglotEvaluate: code in: language [

	^  'Polyglot.eval(', (self codeForEscapedString: language), ', ', (self codeForEscapedString: code), ')'
]

{ #category : #polyglot }
PELanguageInfoJavaScript class >> codeForPolyglotEvaluateFile: path in: language [

	^  'Polyglot.evalFile(', (self codeForEscapedString: language), ', ', (self codeForEscapedString: path), ')'
]

{ #category : #polyglot }
PELanguageInfoJavaScript class >> codeForPolyglotExportOf: value to: name [

	^ 'Polyglot.export(', (self codeForEscapedString: name), ', ', value, ')'
]

{ #category : #polyglot }
PELanguageInfoJavaScript class >> codeForPolyglotHeader [

	^ ''
]

{ #category : #polyglot }
PELanguageInfoJavaScript class >> codeForPolyglotImport: name [

	^ 'Polyglot.import(', (self codeForEscapedString: name), ')'
]

{ #category : #polyglot }
PELanguageInfoJavaScript class >> codeForStatementSeparator [

	^ ';'
]

{ #category : #accessing }
PELanguageInfoJavaScript class >> color [

	^ Color fromString: '#FFEB3B'
]

{ #category : #accessing }
PELanguageInfoJavaScript class >> defaultLineEndings [

	^ String lf
]

{ #category : #accessing }
PELanguageInfoJavaScript class >> extensions [

	^ #(js)
]

{ #category : #accessing }
PELanguageInfoJavaScript class >> id [

	^ self polyglotId
]

{ #category : #accessing }
PELanguageInfoJavaScript class >> languageName [

	^ 'JavaScript'
]

{ #category : #polyglot }
PELanguageInfoJavaScript class >> polyglotId [

	^ Smalltalk globals at: #Polyglot ifPresent: #languageIdJS ifAbsent: [#js]
]

{ #category : #accessing }
PELanguageInfoJavaScript class >> rougeLexerName [

	^ 'javascript'
]

{ #category : #polyglot }
PELanguageInfoJavaScript class >> unescapedCommentContent: aString [

	^ self unescapedCommentContent: aString eachCommentLineStartingWith: '// '
]
