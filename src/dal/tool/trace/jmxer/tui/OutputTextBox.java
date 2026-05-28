package dal.tool.trace.jmxer.tui;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * Read-only Output pane: stores logical result lines and reflows them for display.
 * Ctrl+L toggles wrap (multi-line within pane width) vs single-line (horizontal scroll) without re-fetching results.
 */
final class OutputTextBox extends TextBox implements TuiF2Handler {
	private static final int MAX_LOGICAL_LINES = 2000;

	private final BoxFocusManager mgr;
	private final Panel pane;
	private final JmxTuiOutputHost host;
	private final List<String> logicalLines = new ArrayList<String>();
	private Runnable saveOutputAction;
	private Runnable wrapModeChangeListener;
	private boolean wrapMode;

	OutputTextBox(BoxFocusManager mgr, Panel pane, TerminalSize size, Style style, JmxTuiOutputHost host) {
		super(size, style);
		this.mgr = mgr;
		this.pane = pane;
		this.host = host;
		setVerticalFocusSwitching(false);
		setHorizontalFocusSwitching(true);
	}

	void setSaveOutputAction(Runnable action) {
		this.saveOutputAction = action;
	}

	void setWrapModeChangeListener(Runnable listener) {
		this.wrapModeChangeListener = listener;
	}

	boolean isWrapMode() {
		return wrapMode;
	}

	int getPaneRowsForLayout() {
		return getPaneRows();
	}

	List<String> getLogicalLines() {
		return logicalLines;
	}

	void clearAll() {
		logicalLines.clear();
		replaceDisplayLines(null);
		resetOutputScrollState();
	}

	void appendLogicalLine(String line) {
		if(line == null) {
			line = "";
		}
		logicalLines.add(line);
		trimLogicalLines();
		reflowDisplay();
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
	void reflowDisplay() {
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
		resetOutputScrollState();
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

	private void trimLogicalLines() {
		while(logicalLines.size() > MAX_LOGICAL_LINES) {
			logicalLines.remove(0);
		}
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
		setHorizontalFocusSwitching(!wrapMode);
	}

	/** Reset view offset and caret after content changes; keep Lanterna scrollbars enabled for vertical scroll. */
	private void resetOutputScrollState() {
		TextBox.TextBoxRenderer renderer = getRenderer();
		if(renderer instanceof TextBox.DefaultTextBoxRenderer) {
			TextBox.DefaultTextBoxRenderer defaultRenderer = (TextBox.DefaultTextBoxRenderer) renderer;
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
		return super.handleKeyStroke(keyStroke);
	}

	private static boolean isHorizontalScrollKey(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return false;
		}
		KeyType kt = keyStroke.getKeyType();
		return kt == KeyType.ArrowLeft || kt == KeyType.ArrowRight;
	}
}
