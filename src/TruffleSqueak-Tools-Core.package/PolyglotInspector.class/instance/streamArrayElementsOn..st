fields - streaming
streamArrayElementsOn: aStream
	"Truncate array elements if there are too many of them."
	
	self
		streamOn: aStream
		truncate: (1 to: ((Interop hasArrayElements: self object)
							ifTrue: [ Interop getArraySize: self object ] ifFalse: [ 0 ] ))
		collectFields: [:index |
			(self newFieldForType: #indexed key: index)
				valueGetter: [:object | Interop readArrayElement: object index: index];
				valueSetter: [:object :value | Interop writeArrayElement: object index: index value: value];
				yourself]