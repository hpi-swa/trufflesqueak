helpers
hashEntriesOf: anObject
	| size |
	^ (size := (Interop getHashSize: anObject)) < 100 ifTrue: [ | iterator |
		iterator := Interop getHashEntriesIterator: anObject.
		Text streamContents: [ :aStream |
			[ Interop hasIteratorNextElement: iterator ] whileTrue: [ | next key value |
				next := Interop getIteratorNextElement: iterator.
				key := next at: 1.
				value := next at: 2.
				aStream
					withAttributes: { PluggableTextAttribute evalBlock: [ key inspect ]. TextColor color: Color gray }
						do: [ aStream nextPutAll: key printString ];
					nextPutAll: ' -> ';
					withAttributes: { PluggableTextAttribute evalBlock: [ value inspect ]. TextEmphasis italic }
						do: [ aStream nextPutAll: '', value printString ];
					cr; cr
			] ]]
		ifFalse: [ 'Hash size: ', size, ' (too many entries to display)' ]
