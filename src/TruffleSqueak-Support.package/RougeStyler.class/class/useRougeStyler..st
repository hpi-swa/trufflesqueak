preferences
useRougeStyler: aBoolean

	self useRougeStyler == aBoolean ifFalse: [
		Default := ((aBoolean and: [ Polyglot isPermitted: 'ruby' ])
			ifTrue: [ RougeStylerEnabled ]
			ifFalse: [ RougeStylerDisabled ]) basicNew initialize ]