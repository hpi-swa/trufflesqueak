error handling
doesNotUnderstand: aMessage
	^ [ self send: aMessage ] on: Error do: [
		super doesNotUnderstand: aMessage ]