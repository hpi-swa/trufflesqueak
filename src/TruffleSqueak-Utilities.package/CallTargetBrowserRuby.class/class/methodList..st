helpers
methodList: aModuleName
	^ ((self methodListGetter value: aModuleName) asCollection
			collect: [:ea | ea asString]) sorted