Class {
	#name : #PELanguageInfoPython,
	#superclass : #PELanguageInfoPolyglot,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #polyglot }
PELanguageInfoPython class >> codeForAssignmentOf: value to: name [

	^ name, ' = ', value
]

{ #category : #polyglot }
PELanguageInfoPython class >> codeForComment: string [

	^ self generateOneLineCommentsFor: string startingWith: '# '
]

{ #category : #polyglot }
PELanguageInfoPython class >> codeForEscapedString: string [

	^ string lineCount > 1
		ifTrue: ['"""', (string copyReplaceAll: '"""' with: '\"""'),  '"""']
		ifFalse: ['"', ((string copyReplaceAll: '\' with: '\\') copyReplaceAll: '"' with: '\"'),  '"']
]

{ #category : #polyglot }
PELanguageInfoPython class >> codeForPolyglotEvaluate: code in: language [

	^  'polyglot.eval(language=', (self codeForEscapedString: language), ', string=', (self codeForEscapedString: code), ')'
]

{ #category : #polyglot }
PELanguageInfoPython class >> codeForPolyglotEvaluateFile: path in: language [

	^  'polyglot.eval(language=', (self codeForEscapedString: language), ', path=', (self codeForEscapedString: path), ')'
]

{ #category : #polyglot }
PELanguageInfoPython class >> codeForPolyglotExportOf: value to: name [

	^ 'polyglot.export_value(', value, ', ', (self codeForEscapedString: name), ')'
]

{ #category : #polyglot }
PELanguageInfoPython class >> codeForPolyglotHeader [

	^ 'import polyglot'
]

{ #category : #polyglot }
PELanguageInfoPython class >> codeForPolyglotImport: name [

	^ 'polyglot.import_value(', (self codeForEscapedString: name), ')'
]

{ #category : #polyglot }
PELanguageInfoPython class >> codeForStatementSeparator [

	^ ''
]

{ #category : #accessing }
PELanguageInfoPython class >> color [

	^ Color fromString: '#039BE5'
]

{ #category : #accessing }
PELanguageInfoPython class >> defaultLineEndings [

	^ String lf
]

{ #category : #accessing }
PELanguageInfoPython class >> extensions [

	^ #(py)
]

{ #category : #accessing }
PELanguageInfoPython class >> id [

	^ self polyglotId
]

{ #category : #testing }
PELanguageInfoPython class >> isPython [

	^ true
]

{ #category : #accessing }
PELanguageInfoPython class >> languageName [

	^ 'Python'
]

{ #category : #polyglot }
PELanguageInfoPython class >> polyglotId [

	^ Smalltalk globals at: #Polyglot ifPresent: #languageIdPython ifAbsent: [#python]
]

{ #category : #accessing }
PELanguageInfoPython class >> rougeLexerName [

	^ 'python'
]

{ #category : #polyglot }
PELanguageInfoPython class >> unescapedCommentContent: aString [

	^ self unescapedCommentContent: aString eachCommentLineStartingWith: '// '
]
