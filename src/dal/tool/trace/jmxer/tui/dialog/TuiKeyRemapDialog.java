package dal.tool.trace.jmxer.tui.dialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListener;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeFocusPane;
import dal.tool.trace.jmxer.tui.mode.JmxTuiUiMode;
import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiKeyAction;
import dal.tool.trace.jmxer.tui.support.TuiKeyActionUiMode;
import dal.tool.trace.jmxer.tui.support.TuiKeyBindings;
import dal.tool.trace.jmxer.tui.support.TuiKeyBindings.RebindResult;
import dal.tool.trace.jmxer.tui.support.TuiKeyRemapCatalog;
import dal.tool.trace.jmxer.tui.support.TuiKeyRemapScope;
import dal.tool.trace.jmxer.tui.support.TuiKeyRemapUiKeys;
import dal.tool.trace.jmxer.tui.support.TuiKeyStrokeFormat;
import dal.tool.trace.jmxer.tui.widget.KeyRemapListItemRenderer;
import dal.tool.trace.jmxer.tui.widget.TuiDialogActionListBox;

/**
 * TUI 키 리매핑 다이얼로그. Global·Window 섹션을 한 화면에서 라이브 키 캡처로 변경한다.
 *
 */
public final class TuiKeyRemapDialog extends DialogWindow {

	private static final int CONTENT_HORIZONTAL_PADDING = 2;
	private static final int MIN_CONTENT_COLUMNS = 68;
	private static final int DIALOG_HORIZONTAL_CHROME_COLUMNS = 6;

	private final WindowBasedTextGUI gui;
	private final TuiKeyBindings bindings;
	private final String uiModeId;
	private final int contentColumns;
	private final List<TuiKeyAction> globalActions;
	private final List<TuiKeyAction> windowActions;
	private final TuiDialogActionListBox globalList;
	private final TuiDialogActionListBox windowList;
	private final EmptySpace statusSpacer;
	private final Label statusLabel;
	private final KeyRemapListItemRenderer listRenderer;
	private volatile boolean capturing;
	private volatile TuiKeyAction captureTarget;

	private TuiKeyRemapDialog(WindowBasedTextGUI gui, String title, String description, TuiKeyBindings bindings,
			String uiModeId, List<TuiKeyAction> globalActions, List<TuiKeyAction> windowActions,
			TuiKeyRemapScope windowScope, int contentColumns) {
		super(title);
		this.gui = gui;
		this.bindings = bindings;
		this.uiModeId = uiModeId;
		this.contentColumns = contentColumns;
		this.globalActions = globalActions == null ? new ArrayList<TuiKeyAction>() : globalActions;
		this.windowActions = windowActions == null ? new ArrayList<TuiKeyAction>() : windowActions;
		this.listRenderer = new KeyRemapListItemRenderer();
		enableCenteredHint();

		Panel fieldsPanel = new Panel();
		fieldsPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(0).setRightMarginSize(0));
		TuiDialogLayout.addFormDescription(fieldsPanel, description);

		Panel globalSection = new Panel();
		globalSection.setLayoutManager(new GridLayout(1).setLeftMarginSize(0).setRightMarginSize(0));
		globalSection.addComponent(new Label(TuiKeyRemapUiKeys.GLOBAL_SECTION_LABEL));
		if(this.globalActions.isEmpty()) {
			globalSection.addComponent(new Label(TuiKeyRemapUiKeys.NO_GLOBAL_KEYS_MESSAGE));
			globalList = null;
		} else {
			globalList = new TuiDialogActionListBox(new TerminalSize(contentColumns, this.globalActions.size()));
			globalSection.addComponent(globalList);
		}
		fieldsPanel.addComponent(globalSection);

		if(!this.windowActions.isEmpty()) {
			fieldsPanel.addComponent(new EmptySpace(TerminalSize.ONE));
		}
		windowList = buildWindowList(fieldsPanel, contentColumns, windowScope);
		listRenderer.bindLists(globalList, this.globalActions, windowList, this.windowActions);
		if(globalList != null) {
			globalList.setListItemRenderer(listRenderer);
		}
		if(windowList != null) {
			windowList.setListItemRenderer(listRenderer);
		}

		statusSpacer = new EmptySpace(TerminalSize.ONE);
		statusSpacer.setVisible(false);
		fieldsPanel.addComponent(statusSpacer);
		statusLabel = new Label("");
		statusLabel.setVisible(false);
		fieldsPanel.addComponent(statusLabel);

		Button resetButton = new Button(TuiKeyRemapUiKeys.RESET_LABEL, new Runnable() {
			@Override
			public void run() {
				cancelCapture();
				List<TuiKeyAction> listed = new ArrayList<TuiKeyAction>();
				listed.addAll(TuiKeyRemapDialog.this.globalActions);
				listed.addAll(TuiKeyRemapDialog.this.windowActions);
				bindings.resetActions(listed);
				rebuildActionLists();
				clearStatus();
			}
		});
		Button closeButton = new Button(TuiKeyRemapUiKeys.CLOSE_LABEL, new Runnable() {
			@Override
			public void run() {
				close();
			}
		});

		Panel buttonPanel = new Panel();
		buttonPanel.setLayoutManager(new GridLayout(2).setHorizontalSpacing(1));
		buttonPanel.addComponent(resetButton);
		buttonPanel.addComponent(closeButton);

		Panel mainPanel = new Panel();
		mainPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		mainPanel.addComponent(fieldsPanel);
		mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
		buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false))
			.addTo(mainPanel);
		setComponent(mainPanel);

		rebuildActionLists();
		installCaptureListener();
		TuiDialogEscFocus.install(this);
	}

	public static void show(WindowBasedTextGUI gui, TuiKeyBindings bindings, JmxTuiUiMode mode,
			JmxTuiModeContext ctx, JmxTuiModeFocusPane focusedPane, JmxSettings settings) {
		if(gui == null || bindings == null) {
			return;
		}
		List<TuiKeyAction> globalActions = TuiKeyRemapCatalog.listGlobalActions(mode, ctx, settings, bindings);
		List<TuiKeyAction> windowActions = TuiKeyRemapCatalog.listWindowActions(mode, ctx, focusedPane, settings, bindings);
		if(globalActions.isEmpty() && windowActions.isEmpty()) {
			return;
		}
		TuiKeyRemapScope windowScope = TuiKeyBindings.resolveWindowScope(focusedPane, mode);
		String uiModeId = mode == null ? null : mode.getId();
		int columns = computeContentColumns(gui, bindings, globalActions, windowActions,
			TuiKeyRemapUiKeys.DIALOG_DESCRIPTION);
		TuiKeyRemapDialog dialog = new TuiKeyRemapDialog(
			gui,
			TuiKeyRemapUiKeys.DIALOG_TITLE,
			TuiKeyRemapUiKeys.DIALOG_DESCRIPTION,
			bindings,
			uiModeId,
			globalActions,
			windowActions,
			windowScope,
			columns
		);
		dialog.showDialog(gui);
	}

	@Override
	public Object showDialog(WindowBasedTextGUI textGUI) {
		textGUI.addWindow(this);
		textGUI.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				Interactable focus = globalList != null ? globalList : windowList;
				if(focus == null) {
					return;
				}
				setFocusedInteractable(focus);
				focus.takeFocus();
			}
		});
		waitUntilClosed();
		return null;
	}

	private TuiDialogActionListBox buildWindowList(Panel fieldsPanel, int contentColumns, TuiKeyRemapScope windowScope) {
		if(windowActions.isEmpty()) {
			return null;
		}
		Panel windowSection = new Panel();
		windowSection.setLayoutManager(new GridLayout(1).setLeftMarginSize(0).setRightMarginSize(0));
		String windowLabel = TuiKeyRemapUiKeys.WINDOW_SECTION_LABEL_PREFIX
			+ (windowScope == null ? "Window" : windowScope.getDisplayName())
			+ TuiKeyRemapUiKeys.WINDOW_SECTION_LABEL_SUFFIX;
		windowSection.addComponent(new Label(windowLabel));
		TuiDialogActionListBox list = new TuiDialogActionListBox(new TerminalSize(contentColumns, windowActions.size()));
		windowSection.addComponent(list);
		fieldsPanel.addComponent(windowSection);
		return list;
	}

	private void rebuildActionLists() {
		rebuildActionLists(null);
	}

	private void rebuildActionLists(TuiKeyAction focusAction) {
		TuiDialogActionListBox focusedList = resolveFocusedList();
		int globalIdx = captureSelectedIndex(globalList, globalActions.size());
		int windowIdx = captureSelectedIndex(windowList, windowActions.size());
		if(globalList != null) {
			populateList(globalList, globalActions);
		}
		if(windowList != null) {
			populateList(windowList, windowActions);
		}
		if(focusAction != null) {
			focusListSelection(focusAction);
			return;
		}
		if(globalList != null) {
			globalList.setSelectedIndex(globalIdx);
		}
		if(windowList != null) {
			windowList.setSelectedIndex(windowIdx);
		}
		if(focusedList != null) {
			setFocusedInteractable(focusedList);
			focusedList.takeFocus();
		}
	}

	private TuiDialogActionListBox resolveFocusedList() {
		if(globalList != null && globalList.isFocused()) {
			return globalList;
		}
		if(windowList != null && windowList.isFocused()) {
			return windowList;
		}
		return globalList != null ? globalList : windowList;
	}

	private static int captureSelectedIndex(TuiDialogActionListBox list, int actionCount) {
		if(list == null || actionCount <= 0) {
			return 0;
		}
		int idx = list.getSelectedIndex();
		if(idx < 0) {
			return 0;
		}
		return Math.min(idx, actionCount - 1);
	}

	private void focusListSelection(TuiKeyAction action) {
		if(action == null) {
			return;
		}
		int globalIdx = globalActions.indexOf(action);
		if(globalIdx >= 0 && globalList != null) {
			globalList.setSelectedIndex(globalIdx);
			setFocusedInteractable(globalList);
			globalList.takeFocus();
			return;
		}
		int windowIdx = windowActions.indexOf(action);
		if(windowIdx >= 0 && windowList != null) {
			windowList.setSelectedIndex(windowIdx);
			setFocusedInteractable(windowList);
			windowList.takeFocus();
		}
	}

	private void populateList(TuiDialogActionListBox list, List<TuiKeyAction> actions) {
		list.clearItems();
		for(int i = 0; i < actions.size(); i++) {
			final TuiKeyAction action = actions.get(i);
			list.addItem(formatActionLabel(action), new Runnable() {
				@Override
				public void run() {
					beginCapture(action);
				}
			});
		}
	}

	private String formatActionLabel(TuiKeyAction action) {
		String label = action.getDisplayName() + " - [" + bindings.getActionLabel(action) + "]";
		return TuiKeyRemapUiKeys.indentKeyListLabel(label, bindings.isRemappedFromDefault(action));
	}

	private void beginCapture(TuiKeyAction action) {
		captureTarget = action;
		capturing = true;
		listRenderer.setCaptureTarget(action);
		refreshListAppearance();
		setStatus(TuiKeyRemapUiKeys.CAPTURE_HINT + "  " + action.getDisplayName(), true);
	}

	private void clearStatus() {
		resetStatusStyle();
		statusSpacer.setVisible(false);
		statusLabel.setText("");
		statusLabel.setVisible(false);
	}

	private void setStatus(String text) {
		setStatus(text, false);
	}

	private void setStatus(String text, boolean leadingBlankLine) {
		resetStatusStyle();
		boolean visible = text != null && text.length() > 0;
		statusSpacer.setVisible(visible && leadingBlankLine);
		statusLabel.setText(visible ? text : "");
		statusLabel.setVisible(visible);
	}

	private void resetStatusStyle() {
		statusLabel.setForegroundColor(null);
		statusLabel.setBackgroundColor(null);
		statusLabel.removeStyle(SGR.BOLD);
	}

	private void showCaptureErrorBanner(String reasonLine, String retryLine) {
		statusLabel.setBackgroundColor(JmxTuiTheme.KEY_REMAP_ERROR_BG);
		statusLabel.setForegroundColor(JmxTuiTheme.KEY_REMAP_ERROR_FG);
		statusLabel.addStyle(SGR.BOLD);
		statusSpacer.setVisible(true);
		statusLabel.setText(formatErrorBanner(reasonLine, retryLine));
		statusLabel.setVisible(true);
		ringBellQuietly();
	}

	private String formatErrorBanner(String reasonLine, String retryLine) {
		int targetWidth = Math.max(24, contentColumns);
		int innerWidth = Math.max(20, targetWidth - 2);
		List<String> wrappedLines = new ArrayList<String>();
		appendWrappedBannerLine(wrappedLines, reasonLine, innerWidth);
		appendWrappedBannerLine(wrappedLines, retryLine, innerWidth);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < wrappedLines.size(); i++) {
			if(i > 0) {
				sb.append('\n');
			}
			String line = " " + wrappedLines.get(i);
			int cols = TerminalTextUtils.getColumnWidth(line);
			sb.append(line);
			while(cols < targetWidth) {
				sb.append(' ');
				cols++;
			}
		}
		return sb.toString();
	}

	private static void appendWrappedBannerLine(List<String> out, String text, int maxCols) {
		if(text == null || text.length() == 0) {
			return;
		}
		String remaining = text.trim();
		while(remaining.length() > 0) {
			if(TerminalTextUtils.getColumnWidth(remaining) <= maxCols) {
				out.add(remaining);
				return;
			}
			String fitted = TerminalTextUtils.fitString(remaining, maxCols);
			int breakIdx = fitted.lastIndexOf(' ');
			if(breakIdx > 0) {
				out.add(fitted.substring(0, breakIdx));
				remaining = remaining.substring(breakIdx + 1).trim();
			} else {
				if(fitted.length() == 0) {
					fitted = remaining.substring(0, 1);
				}
				out.add(fitted);
				remaining = remaining.substring(fitted.length()).trim();
			}
		}
	}

	private void ringBellQuietly() {
		if(gui == null) {
			return;
		}
		try {
			Screen screen = gui.getScreen();
			if(screen instanceof TerminalScreen) {
				((TerminalScreen) screen).getTerminal().bell();
			}
		} catch(Exception e) {}
	}

	private void cancelCapture() {
		capturing = false;
		captureTarget = null;
		listRenderer.setCaptureTarget(null);
		refreshListAppearance();
	}

	private void refreshListAppearance() {
		if(globalList != null) {
			globalList.invalidate();
		}
		if(windowList != null) {
			windowList.invalidate();
		}
	}

	private void installCaptureListener() {
		addWindowListener(new WindowListener() {
			@Override
			public void onInput(Window pane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
				if(!capturing || keyStroke == null) {
					return;
				}
				deliverEvent.set(false);
				if(keyStroke.getKeyType() == KeyType.Escape) {
					cancelCapture();
					clearStatus();
					return;
				}
				TuiKeyAction target = captureTarget;
				if(target == null) {
					cancelCapture();
					clearStatus();
					return;
				}
				RebindResult result = bindings.rebind(target, keyStroke, uiModeId);
				if(result == RebindResult.OK) {
					cancelCapture();
					rebuildActionLists(target);
					setStatus("Assigned " + bindings.getActionLabel(target)
						+ " to " + target.getDisplayName() + ".");
					return;
				}
				String pressedLabel = TuiKeyStrokeFormat.formatLabel(keyStroke);
				if(result == RebindResult.RESERVED) {
					showCaptureErrorBanner(
						TuiKeyRemapUiKeys.formatReservedFailure(pressedLabel),
						TuiKeyRemapUiKeys.RETRY_CAPTURE_HINT);
					return;
				}
				if(result == RebindResult.INVALID_RESIZE_KEY) {
					showCaptureErrorBanner(
						TuiKeyRemapUiKeys.formatInvalidResizeFailure(pressedLabel),
						TuiKeyRemapUiKeys.RETRY_RESIZE_CAPTURE_HINT);
					return;
				}
				showCaptureErrorBanner(
					TuiKeyRemapUiKeys.formatConflictFailure(pressedLabel, findConflictAction(keyStroke, target)),
					TuiKeyRemapUiKeys.RETRY_CAPTURE_HINT);
			}

			@Override
			public void onUnhandledInput(Window pane, KeyStroke keyStroke, AtomicBoolean consumeEvent) {
			}

			@Override
			public void onResized(Window window, TerminalSize oldSize, TerminalSize newSize) {
			}

			@Override
			public void onMoved(Window window, com.googlecode.lanterna.TerminalPosition oldPosition, com.googlecode.lanterna.TerminalPosition newPosition) {
			}
		});
	}

	private String findConflictAction(KeyStroke keyStroke, TuiKeyAction exclude) {
		if(exclude == null) {
			return "another action";
		}
		TuiKeyRemapScope scope = exclude.getScope();
		for(TuiKeyAction action : TuiKeyAction.values()) {
			if(action == exclude || !TuiKeyActionUiMode.shareActiveUiMode(uiModeId, exclude, action)) {
				continue;
			}
			if(!TuiKeyBindings.scopesConflict(scope, action.getScope())) {
				continue;
			}
			if(bindings.matches(action, keyStroke)) {
				return formatConflictActionLabel(action);
			}
		}
		return "another action";
	}

	private String formatConflictActionLabel(TuiKeyAction action) {
		if(action == null) {
			return "another action";
		}
		if(action.getScope() == TuiKeyRemapScope.GLOBAL) {
			return action.getDisplayName() + " (Global)";
		}
		TuiKeyRemapScope scope = action.getScope();
		return action.getDisplayName() + " (Window - " + scope.getDisplayName() + ")";
	}

	private static int computeContentColumns(WindowBasedTextGUI gui, TuiKeyBindings bindings,
			List<TuiKeyAction> globalActions, List<TuiKeyAction> windowActions, String description) {
		int maxLen = 0;
		maxLen = Math.max(maxLen, measureActionLabels(bindings, globalActions));
		maxLen = Math.max(maxLen, measureActionLabels(bindings, windowActions));
		maxLen = Math.max(maxLen, TuiKeyRemapUiKeys.NO_GLOBAL_KEYS_MESSAGE.length());

		if(description != null) {
			String[] lines = description.split("\n", -1);
			for(int i = 0; i < lines.length; i++) {
				String line = lines[i];
				if(line != null) {
					maxLen = Math.max(maxLen, line.length());
				}
			}
		}
		maxLen = Math.max(maxLen, TuiKeyRemapUiKeys.GLOBAL_SECTION_LABEL.length());
		maxLen = Math.max(maxLen, TuiKeyRemapUiKeys.RESERVED_MESSAGE.length());
		maxLen = Math.max(maxLen, TuiKeyRemapUiKeys.INVALID_RESIZE_KEY_MESSAGE.length());
		maxLen = Math.max(maxLen, TuiKeyRemapUiKeys.CAPTURE_HINT.length());

		int needed = maxLen + CONTENT_HORIZONTAL_PADDING;
		int terminalCols = resolveTerminalColumns(gui);
		int maxFit = Math.max(MIN_CONTENT_COLUMNS, terminalCols - DIALOG_HORIZONTAL_CHROME_COLUMNS);
		return Math.min(maxFit, Math.max(MIN_CONTENT_COLUMNS, needed));
	}

	private static int measureActionLabels(TuiKeyBindings bindings, List<TuiKeyAction> actions) {
		int maxLen = 0;
		if(actions == null) {
			return maxLen;
		}
		for(int i = 0; i < actions.size(); i++) {
			TuiKeyAction action = actions.get(i);
			String label = TuiKeyRemapUiKeys.indentKeyListLabel(
				action.getDisplayName() + " - [" + bindings.getActionLabel(action) + "]",
				bindings.isRemappedFromDefault(action));
			maxLen = Math.max(maxLen, label.length());
		}
		return maxLen;
	}

	private static int resolveTerminalColumns(WindowBasedTextGUI gui) {
		try {
			Screen screen = gui.getScreen();
			if(screen != null) {
				TerminalSize ts = screen.getTerminalSize();
				if(ts != null) {
					return ts.getColumns();
				}
			}
		} catch(Exception e) {}
		return 120;
	}

	private void enableCenteredHint() {
		Set<Window.Hint> hints = new HashSet<Window.Hint>(getHints());
		hints.add(Window.Hint.CENTERED);
		setHints(hints);
	}
}
