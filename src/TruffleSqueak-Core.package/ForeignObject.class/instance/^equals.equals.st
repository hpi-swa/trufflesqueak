comparing
== anObject
	^ super == anObject or: [ Interop isIdentical: self to: anObject ]