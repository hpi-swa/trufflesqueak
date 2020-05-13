*TruffleSqueak-Tools-Editor-Support
withInternetLineEndings
	"Answer a copy of myself in which all sequences of <CR> or <LF> have been changed to <CR><LF>"
	| newText |
	newText := self copyReplaceAll: String crlf with: String lf asTokens: false.
	newText := newText copyReplaceAll: String cr with: String lf asTokens: false.
	^newText copyReplaceAll: String lf with: String crlf asTokens: false.