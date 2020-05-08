initialization
form
	^ form ifNil: [ form := self newForm ]