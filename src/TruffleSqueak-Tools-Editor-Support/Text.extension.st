Extension { #name : #Text }

{ #category : #'*TruffleSqueak-Tools-Editor-Support' }
Text >> findLastOccurrenceOfString: subString startingAt: start [ 

	^ string findLastOccurrenceOfString: subString startingAt: start
]

{ #category : #'*TruffleSqueak-Tools-Editor-Support' }
Text >> withInternetLineEndings [
	"Answer a copy of myself in which all sequences of <CR> or <LF> have been changed to <CR><LF>"
	| newText |
	newText := self copyReplaceAll: String crlf with: String lf asTokens: false.
	newText := newText copyReplaceAll: String cr with: String lf asTokens: false.
	^newText copyReplaceAll: String lf with: String crlf asTokens: false.
]

{ #category : #'*TruffleSqueak-Tools-Editor-Support' }
Text >> withUnixLineEndings [
	"Answer a copy of myself in which all sequences of <CR><LF> or <CR> have been changed to <LF>"
	| newText |
	(string includes: Character cr) ifFalse: [ ^self copy ].
	newText := self copyReplaceAll: String crlf with: String lf asTokens: false.
	(newText asString includes: Character cr) ifFalse: [ ^newText ].
	^newText copyReplaceAll: String cr with: String lf asTokens: false.
]
