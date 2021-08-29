Class {
	#name : #PEFileExplorerSpec,
	#superclass : #PluggableWidgetSpec,
	#instVars : [
		'getRootDirectories',
		'setSelectedFileEntry'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #building }
PEFileExplorerSpec >> buildWith: aBuilder [

	^ aBuilder buildFileExplorer: self
]

{ #category : #accessing }
PEFileExplorerSpec >> getRootDirectories [

	^ getRootDirectories
]

{ #category : #accessing }
PEFileExplorerSpec >> getRootDirectories: aSymbol [

	getRootDirectories := aSymbol
]

{ #category : #accessing }
PEFileExplorerSpec >> setSelectedFileEntry [

	^ setSelectedFileEntry
]

{ #category : #accessing }
PEFileExplorerSpec >> setSelectedFileEntry: aSymbol [

	setSelectedFileEntry := aSymbol
]
