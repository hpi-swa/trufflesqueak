accessing
default
	"Default := nil"
	^ Default ifNil: [ Default := ((Polyglot isPermitted: #ruby)
		ifTrue: [ RougeStylerEnabled ]
		ifFalse: [ RougeStylerDisabled ])
			basicNew initialize ]