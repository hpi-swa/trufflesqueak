private
openFile

	| url |
	url := 'file://', self fileEntry fullPath.
	browser isConnected
		ifTrue: [browser changeLocation: url]
		ifFalse: [browser connect: url]