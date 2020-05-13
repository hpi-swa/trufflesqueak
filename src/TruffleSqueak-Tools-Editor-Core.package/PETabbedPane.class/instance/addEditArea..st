edit areas
addEditArea: aMorph

	| eventMap |
	editAreas add: aMorph.
	
	eventMap := Dictionary newFrom: {
		#save -> #activeEditAreaContentChanged.
		#lineEndingsChanged -> #activeEditAreaContentChanged.
		#generatePolyglotCodeBox -> #activeEditAreaGeneratePolyglotCodeBox.
		#generatePolyglotEvaluate -> #activeEditAreaGeneratePolyglotEvaluate.
		#generatePolyglotImport -> #activeEditAreaGeneratePolyglotImport.
		#generatePolyglotExport -> #activeEditAreaGeneratePolyglotExport.
		#searchForCode -> #activeEditAreaSearchForCode}.
	
	eventMap keysAndValuesDo: [:incomingEvent :triggeredEvent | aMorph model
		when: incomingEvent 
		send: #triggerEvent:with: 
		to: self
		withArguments: {triggeredEvent. aMorph}].
	
	aMorph model when: #newSnippetDirectory send: #newSnippetDirectory: to: self.
	aMorph model when: #focus send: #makeEditAreaActive: to: self with: aMorph