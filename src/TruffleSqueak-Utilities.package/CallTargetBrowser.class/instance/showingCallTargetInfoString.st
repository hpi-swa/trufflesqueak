what to show
showingCallTargetInfoString
	"Answer a string telling whether the receiver is showing plain source"

	^ (self showingCallTargetInfo
		ifTrue:
			['<yes>']
		ifFalse:
			['<no>']), 'call target'