path utils
make: dest relativeTo: root

	"Given two ABSOLUTE paths using forward slashes, dest and root, return a relative path from root to dest"
	| destParts result rootParts |
	result := './'.
	destParts := dest splitBy: '/'.
	rootParts := root splitBy: '/'.
	
	1 to: rootParts size do: [:i | | destPart rootPart |
		rootPart := rootParts at: i.
		destPart := destParts at: i.
		
		destPart = rootPart ifFalse: [
			rootParts size - i + 1 timesRepeat: [result := result, '../'].
			result := result, ((destParts copyFrom: i to: destParts size) joinSeparatedBy: '/').
			^ result]].
	
	^ result, ((destParts copyFrom: rootParts size + 1 to: destParts size) joinSeparatedBy: '/').