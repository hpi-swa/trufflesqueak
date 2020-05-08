user interface
defaultLabelForInspector
	"Answer the default label to be used for an Inspector window on the receiver."

	^ (Interop hasMetaObject: self)
		ifTrue: [ (Interop getMetaQualifiedName: (Interop getMetaObject: self)) asString ]
		ifFalse: [ super defaultLabelForInspector ]