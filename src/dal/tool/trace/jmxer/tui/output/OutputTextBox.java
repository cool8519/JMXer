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
 * 읽기 전용 Output 패널. 논리 결과 줄을 저장하고 표시용으로 재배치한다.
 * Output Wrap 단축키로 줄바꿈(패널 너비 내 여러 줄)과 한 줄(가로 스크롤) 모드를 전환한다.
 *
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
		// 화살표로 다른 패널로 포커스가 넘어가지 않게 한다. 비줄바꿈 모드에서는 좌/우가 가로 스크롤에 쓰인다.
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
			scrollToEndAfterReflow = true;
			reflowDisplay();
		}
	}

	public void scrollDisplayToEnd() {
		scrollToEndAfterReflow = true;
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

	void appendToLastLogicalLine(String suffix) {
		if(suffix == null || suffix.length() == 0) {
			return;
		}
		if(logicalLines.isEmpty()) {
			logicalLines.add(suffix);
		} else {
			int last = logicalLines.size() - 1;
			logicalLines.set(last, logicalLines.get(last) + suffix);
		}
		scrollToEndAfterReflow = true;
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

	void reflowDisplayScrollToEnd() {
		scrollToEndAfterReflow = true;
		reflowDisplay();
	}

	/**
	 * 현재 패널 너비와 줄바꿈 모드로 logicalLines에서 TextBox 표시 줄을 다시 만든다.
	 */
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
	 * setText로 전체 표시 줄을 교체해 Lanterna longestRow를 다시 계산한다.
	 * removeLine은 longestRow를 줄이지 않아 가로 스크롤이 남을 수 있다.
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
		setHorizontalFocusSwitching(false);
	}

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

	private void resetOutputScrollState() {
		TextBox.TextBoxRenderer renderer = getRenderer();
		if(renderer instanceof TextBox.DefaultTextBoxRenderer) {
			TextBox.DefaultTextBoxRenderer defaultRenderer = (TextBox.DefaultTextBoxRenderer)renderer;
			// 줄바꿈 모드에서도 스크롤바를 숨기지 않는다. Lanterna는 양축을 함께 숨기며,
			// 가로 넘침은 OutputTextLayout 열 예산과 좌/우 키 처리로 막는다.
			defaultRenderer.setHideScrollBars(false);
			defaultRenderer.setViewTopLeft(TerminalPosition.TOP_LEFT_CORNER);
		}
		if(getLineCount() > 0) {
			setCaretPosition(0, 0);
		}
	}

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
		if(JmxTuiKeys.isSaveOutputKey(keyStroke)) {
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
	 * 읽기 전용 TextBox 스크롤은 전체 패널 행을 쓴다. 가로 스크롤바가 있으면 콘텐츠 영역이 한 줄 줄어
	 * End/PageDown만으로는 마지막 줄(예: 잘림 안내)이 화면 밖에 남을 수 있다.
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
