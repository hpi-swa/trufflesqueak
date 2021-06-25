preferences
useRougeStyler: aBoolean

	(Default class == self) == aBoolean ifFalse: [
		Default := ((aBoolean and: [ Polyglot isPermitted: 'ruby' ])
			ifTrue: [ RougeStylerEnabled ]
			ifFalse: [ RougeStylerDisabled ]) basicNew initialize ]