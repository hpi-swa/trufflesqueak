"
I am a File Explorer that shows directories, files and squit repos.
Currently, I am mainly used as part of the Polyglot Code Editor.
"
Class {
	#name : #PEFileExplorerModel,
	#superclass : #Model,
	#instVars : [
		'selectedEntry',
		'rootDirectory'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #accessing }
PEFileExplorerModel >> getChildren: aPCFileEntry [

	^ aPCFileEntry children sort: [:a :b | (a name compare: b name) <= 2]
]

{ #category : #accessing }
PEFileExplorerModel >> hasChildren: aPCFileEntry [
	
	^ aPCFileEntry hasChildren
]

{ #category : #menus }
PEFileExplorerModel >> menu: aMenuMorph [

	self selectedEntry ifNil: [^ nil].
	self selectedEntry isFile ifFalse: [aMenuMorph
		add: 'New File' action: #menuCreateFile;
		add: 'New Directory' action: #menuCreateDirectory].
	
	^ aMenuMorph
		add: 'Rename' action: #menuRename;
		add: 'Delete' action: #menuDelete;
		add: 'Print contents to Transcript' action: #menuPrintToTranscript;
		yourself
]

{ #category : #menus }
PEFileExplorerModel >> menuCreateDirectory [
	
	| name |
	name := UIManager default request: 'Directory Name'.
	name ifNotEmpty: [
		self selectedEntry createDirectory: name.
		self changed: #rootDirectoryList]
]

{ #category : #menus }
PEFileExplorerModel >> menuCreateFile [
	
	| name |
	name := UIManager default request: 'File Name'.
	name ifNotEmpty: [
		self selectedEntry createFile: name.
		self changed: #rootDirectoryList]
]

{ #category : #menus }
PEFileExplorerModel >> menuDelete [
	
	| confirmed |
	confirmed := UIManager default confirm: 'Do you really want to delete "', self selectedEntry name, '"?'.
	confirmed ifTrue: [
		self selectedEntry delete.
		self changed: #rootDirectoryList]
]

{ #category : #menus }
PEFileExplorerModel >> menuPrintToTranscript [

	Transcript clear.
	Transcript show: selectedEntry fileEntry readStream upToEnd
]

{ #category : #menus }
PEFileExplorerModel >> menuRename [
	
	| newName |
	newName := UIManager default request: 'Enter new name.' initialAnswer: self selectedEntry name.
	newName ifNotEmpty: [
		self selectedEntry rename: newName.
		self changed: #rootDirectoryList]
]

{ #category : #accessing }
PEFileExplorerModel >> nameOf: aPCFileEntry [

	^ aPCFileEntry name
]

{ #category : #accessing }
PEFileExplorerModel >> selectedEntry [
	
	^ selectedEntry
]

{ #category : #accessing }
PEFileExplorerModel >> selectedEntry: anObject [
	selectedEntry := anObject.
	"Trigger an event for the code editor to notify it about a newly selected file."
	self triggerEvent: #selectionChanged with: selectedEntry.
	"Trigger an event for ToolBuilder so that the selection is highlighted in blue."
	self changed: #selectedEntry
]
