*TruffleSqueak-Tools-Editor-Support
withUnixLineEndings
	"Answer a copy of myself in which all sequences of <CR><LF> or <CR> have been changed to <LF>"
	| newText |
	(string includes: Character cr) ifFalse: [ ^self copy ].
	newText := self copyReplaceAll: String crlf with: String lf asTokens: false.
	(newText asString includes: Character cr) ifFalse: [ ^newText ].
	^newText copyReplaceAll: String cr with: String lf asTokens: false.