accessing
getChildren: aPCFileEntry

	^ aPCFileEntry children sort: [:a :b | (a name compare: b name) <= 2]