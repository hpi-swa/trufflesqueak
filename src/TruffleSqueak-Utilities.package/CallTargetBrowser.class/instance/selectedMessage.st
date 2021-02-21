message list
selectedMessage
	contents == nil ifFalse: [^ contents copy].

	self showingCallTargetInfo ifTrue:
		[ ^ self callTargetInfoContents ].

	^ super selectedMessage