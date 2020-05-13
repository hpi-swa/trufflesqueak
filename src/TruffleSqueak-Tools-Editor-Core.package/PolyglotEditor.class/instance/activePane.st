panes
activePane

	^ self panes detect: [:each | each isVisibleEditAreaActive]