accessing
contents: aString
	super contents: aString.
	Project current addDeferredUIMessage: [ self applyStyle ].