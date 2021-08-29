Class {
	#name : #PELanguageInfoRuby,
	#superclass : #PELanguageInfoPolyglot,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #polyglot }
PELanguageInfoRuby class >> codeForAssignmentOf: value to: name [

	^ name, ' = ', value
]

{ #category : #polyglot }
PELanguageInfoRuby class >> codeForComment: string [

	^ self generateOneLineCommentsFor: string startingWith: '# '
]

{ #category : #polyglot }
PELanguageInfoRuby class >> codeForEscapedString: string [

	string lineCount > 1 ifTrue: [UIManager inform: 'Cannot properly escape multiline string for JavaScript'].
	^ '"', (string copyReplaceAll: '"' with: '\"'),  '"'
]

{ #category : #polyglot }
PELanguageInfoRuby class >> codeForPolyglotEvaluate: code in: language [

	^  'Polyglot.eval(', (self codeForEscapedString: language), ', ', (self codeForEscapedString: code), ')'
]

{ #category : #polyglot }
PELanguageInfoRuby class >> codeForPolyglotEvaluateFile: path in: language [

	^  'Polyglot.eval_file(', (self codeForEscapedString: language), ', ', (self codeForEscapedString: path), ')'
]

{ #category : #polyglot }
PELanguageInfoRuby class >> codeForPolyglotExportOf: value to: name [

	^ 'Polyglot.export(', (self codeForEscapedString: name), ', ', value, ')'
]

{ #category : #polyglot }
PELanguageInfoRuby class >> codeForPolyglotHeader [

	^ ''
]

{ #category : #polyglot }
PELanguageInfoRuby class >> codeForPolyglotImport: name [

	^ 'Polyglot.import(', (self codeForEscapedString: name), ')'
]

{ #category : #polyglot }
PELanguageInfoRuby class >> codeForStatementSeparator [

	^ ''
]

{ #category : #accessing }
PELanguageInfoRuby class >> color [

	^ Color fromString:  '#E57373'
]

{ #category : #accessing }
PELanguageInfoRuby class >> defaultLineEndings [

	^ String lf
]

{ #category : #accessing }
PELanguageInfoRuby class >> extensions [

	^ #(rb)
]

{ #category : #accessing }
PELanguageInfoRuby class >> id [

	^ self polyglotId
]

{ #category : #accessing }
PELanguageInfoRuby class >> languageName [

	^ 'Ruby'
]

{ #category : #polyglot }
PELanguageInfoRuby class >> polyglotId [

	^ Smalltalk globals at: #Polyglot ifPresent: #languageIdRuby ifAbsent: [#ruby]
]

{ #category : #accessing }
PELanguageInfoRuby class >> rougeLexerName [

	^ 'ruby'
]

{ #category : #polyglot }
PELanguageInfoRuby class >> unescapedCommentContent: aString [

	^ self unescapedCommentContent: aString eachCommentLineStartingWith: '# '
]
