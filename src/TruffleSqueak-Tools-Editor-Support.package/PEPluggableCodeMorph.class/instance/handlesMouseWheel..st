event handling
handlesMouseWheel: evt

	^ autoHeight ifTrue: [false] ifFalse: [super handlesMouseWheel: evt]