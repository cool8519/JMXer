package dal.tool.trace.jmxer.tui.dialog;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.LinearLayout.Alignment;
import com.googlecode.lanterna.gui2.LinearLayout.GrowPolicy;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListener;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.trace.jmxer.tui.support.ThreadPaneSortColumn;
import dal.tool.trace.jmxer.tui.support.ThreadPaneSortOrder;
import dal.tool.trace.jmxer.tui.support.ThreadPaneSortSettings;
import dal.tool.trace.jmxer.tui.support.TuiThreadUiKeys;
import dal.tool.trace.jmxer.tui.widget.DialogArrowListItemRenderer;
import dal.tool.trace.jmxer.tui.widget.TuiDialogActionListBox;

/**
 * Thread UI 모드 정렬 다이얼로그. 열과 오름차순/내림차순을 선택한다.
 *
 */
public final class TuiThreadSortDialog extends DialogWindow {

	private static final ThreadPaneSortColumn[] SORT_COLUMNS = new ThreadPaneSortColumn[] {
		ThreadPaneSortColumn.TID,
		ThreadPaneSortColumn.STATE,
		ThreadPaneSortColumn.CPU,
		ThreadPaneSortColumn.MEM,
		ThreadPaneSortColumn.NAME
	};

	private static final ThreadPaneSortOrder[] SORT_ORDERS = new ThreadPaneSortOrder[] {
		ThreadPaneSortOrder.ASCENDING,
		ThreadPaneSortOrder.DESCENDING
	};

	private static final int COLUMN_LIST_ROWS = SORT_COLUMNS.length;
	private static final int SELECTION_LIST_HEIGHT = COLUMN_LIST_ROWS;
	private static final int DIALOG_CONTENT_COLUMNS = 56;
	private static final int SELECTION_ROW_SPACING = 2;
	private static final int HALF_LIST_COLUMNS =
		(DIALOG_CONTENT_COLUMNS - SELECTION_ROW_SPACING) / 2;
	private static final int SELECTION_ROW_ROWS = SELECTION_LIST_HEIGHT + 1;

	private static final DialogArrowListItemRenderer DIALOG_LIST_RENDERER = new DialogArrowListItemRenderer();

	private ThreadPaneSortSettings result;
	private final TuiDialogActionListBox columnList;
	private final TuiDialogActionListBox orderList;
	private final Label selectionLabel;

	private final Runnable columnItemAction = new Runnable() {
		@Override
		public void run() {
			refreshSelectionLabel();
			focusOrderList();
		}
	};

	private final Runnable orderItemAction = new Runnable() {
		@Override
		public void run() {
			refreshSelectionLabel();
			applySelection();
		}
	};

	private TuiThreadSortDialog(ThreadPaneSortSettings initial) {
		super(TuiThreadUiKeys.SORT_DIALOG_TITLE);
		enableCenteredHint();

		ThreadPaneSortSettings initialSettings = initial == null ? ThreadPaneSortSettings.DEFAULT : initial;

		Panel fieldsPanel = new Panel();
		fieldsPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		TuiDialogLayout.addFormDescription(fieldsPanel, TuiThreadUiKeys.SORT_DIALOG_DESCRIPTION);

		Panel columnPanel = new Panel(new LinearLayout(Direction.VERTICAL));
		columnPanel.addComponent(new Label(TuiThreadUiKeys.SORT_DIALOG_COLUMN_LABEL));
		columnList = createSortList(HALF_LIST_COLUMNS, SELECTION_LIST_HEIGHT);
		for(int i = 0; i < SORT_COLUMNS.length; i++) {
			columnList.addItem(SORT_COLUMNS[i].getDisplayLabel(), columnItemAction);
		}
		columnList.setSelectedIndex(indexOfColumn(initialSettings.getColumn()));
		columnPanel.addComponent(columnList);

		Panel orderPanel = new Panel(new LinearLayout(Direction.VERTICAL));
		orderPanel.addComponent(new Label(TuiThreadUiKeys.SORT_DIALOG_ORDER_LABEL));
		orderList = createSortList(HALF_LIST_COLUMNS, SELECTION_LIST_HEIGHT);
		for(int i = 0; i < SORT_ORDERS.length; i++) {
			orderList.addItem(SORT_ORDERS[i].getDisplayLabel(), orderItemAction);
		}
		orderList.setSelectedIndex(indexOfOrder(initialSettings.getOrder()));
		orderPanel.addComponent(orderList);

		Panel selectionRowPanel = new Panel(new LinearLayout(Direction.HORIZONTAL).setSpacing(SELECTION_ROW_SPACING));
		selectionRowPanel.setPreferredSize(new TerminalSize(DIALOG_CONTENT_COLUMNS, SELECTION_ROW_ROWS));
		selectionRowPanel.addComponent(
			columnPanel,
			LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow)
		);
		selectionRowPanel.addComponent(
			orderPanel,
			LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow)
		);
		fieldsPanel.addComponent(selectionRowPanel);

		selectionLabel = new Label(formatSelectionLabel(initialSettings));
		fieldsPanel.addComponent(new EmptySpace(TerminalSize.ONE));
		fieldsPanel.addComponent(selectionLabel);

		Button applyButton = new Button(TuiThreadUiKeys.SORT_DIALOG_APPLY_LABEL, new Runnable() {
			@Override
			public void run() {
				applySelection();
			}
		});
		Button cancelButton = new Button(TuiThreadUiKeys.SORT_DIALOG_CANCEL_LABEL, new Runnable() {
			@Override
			public void run() {
				result = null;
				close();
			}
		});

		Panel buttonPanel = new Panel();
		buttonPanel.setLayoutManager(new GridLayout(2).setHorizontalSpacing(1));
		buttonPanel.addComponent(applyButton);
		buttonPanel.addComponent(cancelButton);

		Panel mainPanel = new Panel();
		mainPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		mainPanel.addComponent(fieldsPanel);
		mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
		buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false))
			.addTo(mainPanel);
		setComponent(mainPanel);
		TuiDialogEscFocus.install(this);
		installSelectionRefreshOnArrowKeys();
	}

	public static ThreadPaneSortSettings show(WindowBasedTextGUI gui, ThreadPaneSortSettings initial) {
		if(gui == null) {
			return null;
		}
		TuiThreadSortDialog dialog = new TuiThreadSortDialog(initial);
		dialog.showDialog(gui);
		return dialog.result;
	}

	@Override
	public Object showDialog(WindowBasedTextGUI textGUI) {
		textGUI.addWindow(this);
		textGUI.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				setFocusedInteractable(columnList);
				columnList.takeFocus();
			}
		});
		waitUntilClosed();
		return result;
	}

	private static TuiDialogActionListBox createSortList(int columns, int rows) {
		TuiDialogActionListBox list = new TuiDialogActionListBox(new TerminalSize(columns, rows));
		list.setListItemRenderer(DIALOG_LIST_RENDERER);
		return list;
	}

	private void focusOrderList() {
		setFocusedInteractable(orderList);
		orderList.takeFocus();
	}

	private void applySelection() {
		result = readSelection();
		close();
	}

	private void refreshSelectionLabel() {
		selectionLabel.setText(formatSelectionLabel(readSelection()));
	}

	private void installSelectionRefreshOnArrowKeys() {
		addWindowListener(new WindowListener() {
			@Override
			public void onInput(Window pane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
				if(!isArrowKey(keyStroke)) {
					return;
				}
				Interactable focused = pane.getFocusedInteractable();
				if(focused != columnList && focused != orderList) {
					return;
				}
				pane.getTextGUI().getGUIThread().invokeLater(new Runnable() {
					@Override
					public void run() {
						refreshSelectionLabel();
					}
				});
			}

			@Override
			public void onUnhandledInput(Window pane, KeyStroke keyStroke, AtomicBoolean consumeEvent) {
			}

			@Override
			public void onResized(Window window, com.googlecode.lanterna.TerminalSize oldSize, com.googlecode.lanterna.TerminalSize newSize) {
			}

			@Override
			public void onMoved(Window window, com.googlecode.lanterna.TerminalPosition oldPosition, com.googlecode.lanterna.TerminalPosition newPosition) {
			}
		});
	}

	private static boolean isArrowKey(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return false;
		}
		KeyType type = keyStroke.getKeyType();
		return type == KeyType.ArrowUp || type == KeyType.ArrowDown;
	}

	private ThreadPaneSortSettings readSelection() {
		int columnIdx = columnList.getSelectedIndex();
		int orderIdx = orderList.getSelectedIndex();
		ThreadPaneSortColumn column = columnIdx >= 0 && columnIdx < SORT_COLUMNS.length
			? SORT_COLUMNS[columnIdx]
			: ThreadPaneSortColumn.TID;
		ThreadPaneSortOrder order = orderIdx >= 0 && orderIdx < SORT_ORDERS.length
			? SORT_ORDERS[orderIdx]
			: ThreadPaneSortOrder.ASCENDING;
		return new ThreadPaneSortSettings(column, order);
	}

	private static String formatSelectionLabel(ThreadPaneSortSettings settings) {
		ThreadPaneSortSettings s = settings == null ? ThreadPaneSortSettings.DEFAULT : settings;
		return TuiThreadUiKeys.SORT_DIALOG_SELECTION_LABEL_PREFIX
			+ s.getColumn().getDisplayLabel()
			+ " / "
			+ s.getOrder().getDisplayLabel();
	}

	private static int indexOfColumn(ThreadPaneSortColumn column) {
		ThreadPaneSortColumn target = column == null ? ThreadPaneSortColumn.TID : column;
		for(int i = 0; i < SORT_COLUMNS.length; i++) {
			if(SORT_COLUMNS[i] == target) {
				return i;
			}
		}
		return 0;
	}

	private static int indexOfOrder(ThreadPaneSortOrder order) {
		ThreadPaneSortOrder target = order == null ? ThreadPaneSortOrder.ASCENDING : order;
		for(int i = 0; i < SORT_ORDERS.length; i++) {
			if(SORT_ORDERS[i] == target) {
				return i;
			}
		}
		return 0;
	}

	private void enableCenteredHint() {
		Set<Window.Hint> hints = new HashSet<Window.Hint>(getHints());
		hints.add(Window.Hint.CENTERED);
		setHints(hints);
	}
}
