overrides
fieldList
	"Answer the base field list plus an abbreviated list of indices."

	(Interop hasArrayElements: object) ifFalse: [^ self baseFieldList].
	^ self baseFieldList ,
		(object basicSize <= (self i1 + self i2)
			ifTrue: [(1 to: object basicSize)
						collect: [:i | i printString]]
			ifFalse: [(1 to: self i1) , (object basicSize-(self i2-1) to: object basicSize)
						collect: [:i | i printString]])