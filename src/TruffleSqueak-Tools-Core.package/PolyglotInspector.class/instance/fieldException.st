fields
fieldException

	^ (self newFieldForType: #misc key: #exception)
		name: 'language info'; emphasizeName;
		valueGetter: [ :o | self exceptionInfoFor: o ]; printValueAsIs;
		yourself