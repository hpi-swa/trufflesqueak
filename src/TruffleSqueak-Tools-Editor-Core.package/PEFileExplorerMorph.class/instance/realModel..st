accessing
realModel: anObject

	realModel ifNotNil: [realModel removeDependent: self].
	anObject ifNotNil: [anObject addDependent: self].
	realModel := anObject.
