setup
register
	(self primitiveRegisterForeignObjectClass)
		ifFalse: [ self error: 'Failed to register ', self name ,' class' ]
