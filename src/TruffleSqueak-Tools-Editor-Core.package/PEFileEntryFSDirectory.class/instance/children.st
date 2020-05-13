accessing
children

	^ self fileDirectory entries collect: [:each | each isDirectory 
		ifTrue: [PEFileEntryFSDirectory on: each asFileDirectory]
		ifFalse: [PEFileEntryFSFile on: each]]