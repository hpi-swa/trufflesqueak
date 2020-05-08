accessing
formatter
	^ formatter ifNil: [ formatter := self newRougeFormatter ]