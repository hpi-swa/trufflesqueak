fields
fieldException

	^ (self newFieldForType: #misc key: #exception)
		name: 'exception info'; emphasizeName;
		valueGetter: [ :o | self exceptionInfoFor: o ]; printValueAsIs;
		yourself