fields
fieldHashEntries

	^ (self newFieldForType: #misc key: #hashes)
		name: 'hash entries'; emphasizeName;
		valueGetter: [ :o | self hashEntriesOf: o ]; printValueAsIs;
		yourself