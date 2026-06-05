package dal.tool.trace.jmxer.tui.output;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.trace.jmxer.tui.support.BoxFocusManager;
import dal.tool.trace.jmxer.tui.support.JmxTuiKeys;
import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiF2Handler;

/**
 * Read-only Output pane: stores logical result lines and reflows them for display.
 * Ctrl+L toggles wrap (multi-line within pane width) vs single-line (horizontal scroll) without re-fetching results.
 */
public final class OutputTextBox extends TextBox implements TuiF2Handler {

	private final BoxFocusManager mgr;
	private final Panel pane;
	private final JmxTuiOutputHost host;
	private final List<String> logicalLines = new ArrayList<String>();
	private Runnable saveOutputAction;
	private Runnable wrapModeChangeListener;
	private boolean wrapMode;
	private int batchAppendDepth;
	private boolean scrollToEndAfterReflow;

	public OutputTextBox(BoxFocusManager mgr, Panel pane, TerminalSize size, Style style, JmxTuiOutputHost host) {
		super(size, style);
		this.mgr = mgr;
		this.pane = pane;
		this.host = host;
		setVerticalFocusSwitching(false);
		// Never move focus to other panes via arrow keys. In non-wrap mode, left/right are used for horizontal scroll.
		setHorizontalFocusSwitching(false);
	}

	@Override
	protected TextBox.TextBoxRenderer createDefaultRenderer() {
		return new OutputTextBoxRenderer();
	}

	public void setSaveOutputAction(Runnable action) {
		this.saveOutputAction = action;
	}

	public void setWrapModeChangeListener(Runnable listener) {
		this.wrapModeChangeListener = listener;
	}

	public boolean isWrapMode() {
		return wrapMode;
	}

	int getPaneRowsForLayout() {
		return getPaneRows();
	}

	public List<String> getLogicalLines() {
		return logicalLines;
	}

	public void clearAll() {
		scrollToEndAfterReflow = false;
		logicalLines.clear();
		replaceDisplayLines(null);
		resetOutputScrollState();
	}

	public void beginBatchAppend() {
		batchAppendDepth++;
	}

	public void endBatchAppend() {
		if(batchAppendDepth <= 0) {
			return;
		}
		batchAppendDepth--;
		if(batchAppendDepth == 0) {
			reflowDisplay();
		}
	}

	void appendLogicalLine(String line) {
		if(line == null) {
			line = "";
		}
		logicalLines.add(line);
		scrollToEndAfterReflow = true;
		if(batchAppendDepth == 0) {
			reflowDisplay();
		}
	}

	void appendLogicalLines(List<String> lines) {
		if(lines == null || lines.isEmpty()) {
			return;
		}
		for(int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if(line == null) {
				line = "";
			}
			logicalLines.add(line);
		}
		scrollToEndAfterReflow = true;
		if(batchAppendDepth == 0) {
			reflowDisplay();
		}
	}

	void removeLastLogicalLines(int count) {
		if(count <= 0) {
			return;
		}
		for(int i = 0; i < count && !logicalLines.isEmpty(); i++) {
			logicalLines.remove(logicalLines.size() - 1);
		}
		reflowDisplay();
	}

	void toggleWrapMode() {
		wrapMode = !wrapMode;
		applyWrapModeNavigation();
		reflowDisplay();
		Runnable listener = wrapModeChangeListener;
		if(listener != null) {
			listener.run();
		}
	}

	/** Rebuild TextBox lines from {@link #logicalLines} using the current pane width and wrap mode. */
	public void reflowDisplay() {
		int paneCols = host == null ? 80 : host.getOutputInnerColumnsAtWrite();
		int paneRows = getPaneRows();
		int contentCols = OutputTextLayout.contentMaxColumns(paneCols, paneRows, logicalLines.size());
		List<String> display = null;
		for(int pass = 0; pass < 3; pass++) {
			resyncRequestSeparators(contentCols);
			display = OutputLineReflow.toDisplayLines(logicalLines, wrapMode, contentCols);
			int next = OutputTextLayout.contentMaxColumns(paneCols, paneRows, display.size());
			if(next == contentCols) {
				break;
			}
			contentCols = next;
		}
		replaceDisplayLines(display);
		if(scrollToEndAfterReflow) {
			scrollToEndAfterReflow = false;
			scrollViewToEnd();
		} else {
			resetOutputScrollState();
		}
		invalidate();
	}

	/**
	 * Replace all visible lines via {@link #setText(String)} so Lanterna recalculates {@code longestRow}.
	 * {@link #removeLine(int)} does not shrink {@code longestRow}, which would leave horizontal scroll stuck.
	 */
	private void replaceDisplayLines(List<String> display) {
		if(display == null || display.isEmpty()) {
			setText("");
			return;
		}
		StringBuilder text = new StringBuilder();
		for(int i = 0; i < display.size(); i++) {
			if(i > 0) {
				text.append('\n');
			}
			String line = display.get(i);
			text.append(line == null ? "" : line);
		}
		setText(text.toString());
	}

	private int getPaneRows() {
		try {
			TerminalSize sz = getSize();
			if(sz != null && sz.getRows() > 0) {
				return sz.getRows();
			}
		} catch(Exception e) {}
		return 1;
	}

	private void applyWrapModeNavigation() {
		// Keep focus switching disabled regardless of wrap mode.
		setHorizontalFocusSwitching(false);
	}

	/** Scroll so the last display line is visible (e.g. after new ERROR output during reconnect). */
	private void scrollViewToEnd() {
		TextBox.TextBoxRenderer renderer = getRenderer();
		if(!(renderer instanceof TextBox.DefaultTextBoxRenderer)) {
			return;
		}
		TerminalSize paneSize = getSize();
		if(paneSize == null || paneSize.getRows() <= 0) {
			return;
		}
		TextBox.DefaultTextBoxRenderer defaultRenderer = (TextBox.DefaultTextBoxRenderer)renderer;
		defaultRenderer.setHideScrollBars(false);
		int maxViewTopRow = OutputTextBoxViewport.maxViewTopRow(this, paneSize);
		defaultRenderer.setViewTopLeft(TerminalPosition.TOP_LEFT_CORNER.withRow(maxViewTopRow));
		int lastLine = Math.max(0, getLineCount() - 1);
		setCaretPosition(0, lastLine);
	}

	/** Reset view offset and caret after content changes; keep Lanterna scrollbars enabled for vertical scroll. */
	private void resetOutputScrollState() {
		TextBox.TextBoxRenderer renderer = getRenderer();
		if(renderer instanceof TextBox.DefaultTextBoxRenderer) {
			TextBox.DefaultTextBoxRenderer defaultRenderer = (TextBox.DefaultTextBoxRenderer)renderer;
			// Do not hide scrollbars in wrap mode: Lanterna hides both axes; horizontal overflow is
			// prevented by OutputTextLayout column budget and left/right key handling instead.
			defaultRenderer.setHideScrollBars(false);
			defaultRenderer.setViewTopLeft(TerminalPosition.TOP_LEFT_CORNER);
		}
		if(getLineCount() > 0) {
			setCaretPosition(0, 0);
		}
	}

	/** Resize dash lines under echoed {@code > command} preambles to the current pane width. */
	private void resyncRequestSeparators(int columns) {
		for(int i = 1; i < logicalLines.size(); i++) {
			String prev = logicalLines.get(i - 1);
			String line = logicalLines.get(i);
			if(prev != null && prev.startsWith("> ") && isRequestSeparatorLine(line)) {
				logicalLines.set(i, JmxTuiOutputSink.buildOutputRequestSeparator(columns));
			}
		}
	}

	private static boolean isRequestSeparatorLine(String line) {
		return line != null && line.matches("^[-=]{3,}$");
	}

	@Override
	public boolean onF2Key() {
		return false;
	}

	@Override
	protected void afterEnterFocus(Interactable.FocusChangeDirection direction, Interactable prev) {
		setTheme(JmxTuiTheme.FOCUSED_TEXTBOX_THEME);
		mgr.setActive(pane);
		super.afterEnterFocus(direction, prev);
		pane.invalidate();
		invalidate();
	}

	@Override
	protected void afterLeaveFocus(Interactable.FocusChangeDirection direction, Interactable next) {
		setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);
		super.afterLeaveFocus(direction, next);
		pane.invalidate();
		invalidate();
	}

	@Override
	public synchronized Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
		if(JmxTuiKeys.isGlobalShortcut(keyStroke)) {
			return Interactable.Result.UNHANDLED;
		}
		if(JmxTuiKeys.isCtrlTogglePrettyPrint(keyStroke)) {
			return Interactable.Result.UNHANDLED;
		}
		if(JmxTuiKeys.isCtrlSuspend(keyStroke)) {
			return Interactable.Result.HANDLED;
		}
		if(JmxTuiKeys.isCtrlSaveOutput(keyStroke)) {
			Runnable action = saveOutputAction;
			if(action != null) {
				action.run();
			}
			return Interactable.Result.HANDLED;
		}
		if(JmxTuiKeys.isCtrlToggleOutputWrap(keyStroke)) {
			toggleWrapMode();
			return Interactable.Result.HANDLED;
		}
		if(wrapMode && isHorizontalScrollKey(keyStroke)) {
			return Interactable.Result.HANDLED;
		}
		Interactable.Result scroll = handleViewportScrollKey(keyStroke);
		if(scroll != null) {
			return scroll;
		}
		Interactable.Result r = super.handleKeyStroke(keyStroke);
		if(isArrowKey(keyStroke) && JmxTuiKeys.isFocusTraversalFromListNavigation(keyStroke, r)) {
			return Interactable.Result.HANDLED;
		}
		return r;
	}

	private static boolean isArrowKey(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return false;
		}
		KeyType kt = keyStroke.getKeyType();
		return kt == KeyType.ArrowUp || kt == KeyType.ArrowDown || kt == KeyType.ArrowLeft || kt == KeyType.ArrowRight;
	}

	private static boolean isHorizontalScrollKey(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return false;
		}
		KeyType kt = keyStroke.getKeyType();
		return kt == KeyType.ArrowLeft || kt == KeyType.ArrowRight;
	}

	/**
	 * Read-only Lanterna {@link TextBox} scroll uses full pane rows; when a horizontal scrollbar is shown the
	 * content area is one row shorter, so End/PageDown can leave the last line (e.g. truncation notice) off-screen.
	 */
	private Interactable.Result handleViewportScrollKey(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return null;
		}
		KeyType kt = keyStroke.getKeyType();
		if(kt != KeyType.Home && kt != KeyType.End && kt != KeyType.PageUp && kt != KeyType.PageDown
				&& kt != KeyType.ArrowUp && kt != KeyType.ArrowDown) {
			return null;
		}
		TextBox.TextBoxRenderer renderer = getRenderer();
		if(!(renderer instanceof TextBox.DefaultTextBoxRenderer)) {
			return null;
		}
		TextBox.DefaultTextBoxRenderer defaultRenderer = (TextBox.DefaultTextBoxRenderer)renderer;
		TerminalSize paneSize = getSize();
		TerminalPosition viewTopLeft = defaultRenderer.getViewTopLeft();
		int maxViewTopRow = OutputTextBoxViewport.maxViewTopRow(this, paneSize);
		int pageStep = OutputTextBoxViewport.contentAreaSize(paneSize, this).getRows();
		switch(kt) {
			case Home:
				defaultRenderer.setViewTopLeft(TerminalPosition.TOP_LEFT_CORNER);
				return Interactable.Result.HANDLED;
			case End:
				defaultRenderer.setViewTopLeft(TerminalPosition.TOP_LEFT_CORNER.withRow(maxViewTopRow));
				return Interactable.Result.HANDLED;
			case PageUp:
				defaultRenderer.setViewTopLeft(viewTopLeft.withRow(Math.max(0, viewTopLeft.getRow() - pageStep)));
				return Interactable.Result.HANDLED;
			case PageDown:
				defaultRenderer.setViewTopLeft(viewTopLeft.withRow(Math.min(maxViewTopRow, viewTopLeft.getRow() + pageStep)));
				return Interactable.Result.HANDLED;
			case ArrowUp:
				if(viewTopLeft.getRow() == 0 && isVerticalFocusSwitching()) {
					return Interactable.Result.MOVE_FOCUS_UP;
				}
				defaultRenderer.setViewTopLeft(viewTopLeft.withRow(Math.max(0, viewTopLeft.getRow() - 1)));
				return Interactable.Result.HANDLED;
			case ArrowDown:
				if(viewTopLeft.getRow() >= maxViewTopRow && isVerticalFocusSwitching()) {
					return Interactable.Result.MOVE_FOCUS_DOWN;
				}
				defaultRenderer.setViewTopLeft(viewTopLeft.withRow(Math.min(maxViewTopRow, viewTopLeft.getRow() + 1)));
				return Interactable.Result.HANDLED;
			default:
				return null;
		}
	}
}
