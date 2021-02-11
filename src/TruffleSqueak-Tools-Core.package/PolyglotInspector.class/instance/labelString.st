user interface - window
labelString
	"See #windowTitle. All tools chose to implement #labelString."
	
	^ '{1}{2}' format: {
		self object defaultLabelForInspector.
		(Interop hasLanguage: self object)
			ifTrue: [' (', (Interop getLanguageInfo: self object) getName ,')']
			ifFalse: ['']}