code actions
setupCodeBoxEventsFor: aPCCodeBox

	aPCCodeBox
		"Triggering the change event activates the hasUnacceptedEdits marker"
		when: #codeBoxChanged
		send: #changed:
		to: self
		with: #unsavedCode;
		
		"If another code box is added within this code box, setup events for that code box as well"
		when: #codeBoxAdded
		send: #setupCodeBoxEventsFor:
		to: self