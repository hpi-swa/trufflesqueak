bottom bar
bottomBarLastModified

	^ self activeEditArea fileEntry
		ifNil: ['']
		ifNotNil: [ | lastModified |
			lastModified := self activeEditArea fileEntry lastModified.
			lastModified asDate yyyymmdd, ' ', lastModified asTime]