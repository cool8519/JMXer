package dal.tool.trace.jmxer.tui.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.ThemeDefinition;
import com.googlecode.lanterna.gui2.AbstractComponent;
import com.googlecode.lanterna.gui2.ComponentRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;

/**
 * 여러 색상 세그먼트로 한 줄 상태 라벨을 렌더링한다.
 *
 */
public final class TuiStatusSegmentLabel extends AbstractComponent<TuiStatusSegmentLabel> {

	/**
	 * 상태 라벨의 텍스트·전경색 한 조각.
	 *
	 */
	public static final class Segment {
		private final String text;
		private final TextColor foreground;

		public Segment(String text, TextColor foreground) {
			this.text = text == null ? "" : text;
			this.foreground = foreground;
		}

		public String getText() {
			return text;
		}

		public TextColor getForeground() {
			return foreground;
		}
	}

	private final List<Segment> segments = new ArrayList<Segment>();
	private int maxColumns = Integer.MAX_VALUE;
	private Integer labelWidth;

	public TuiStatusSegmentLabel() {}

	public void setLabelWidth(int width) {
		labelWidth = Integer.valueOf(width < 0 ? 0 : width);
		setMaxColumns(labelWidth.intValue());
		setPreferredSize(new TerminalSize(labelWidth.intValue(), 1));
	}

	public void setMaxColumns(int maxColumns) {
		this.maxColumns = maxColumns < 0 ? 0 : maxColumns;
		invalidate();
	}

	public void setText(String text) {
		segments.clear();
		segments.add(new Segment(text, null));
		invalidate();
	}

	public void setSegments(List<Segment> newSegments) {
		segments.clear();
		if(newSegments != null) {
			for(int i = 0; i < newSegments.size(); i++) {
				Segment segment = newSegments.get(i);
				if(segment != null) {
					segments.add(segment);
				}
			}
		}
		if(segments.isEmpty()) {
			segments.add(new Segment("", null));
		}
		invalidate();
	}

	public List<Segment> getSegments() {
		return Collections.unmodifiableList(segments);
	}

	@Override
	protected ComponentRenderer<TuiStatusSegmentLabel> createDefaultRenderer() {
		return new SegmentRenderer();
	}

	@Override
	protected TerminalSize calculatePreferredSize() {
		int widthLimit = maxColumns;
		if(labelWidth != null && labelWidth.intValue() < widthLimit) {
			widthLimit = labelWidth.intValue();
		}
		int width = 0;
		for(int i = 0; i < segments.size(); i++) {
			width += TerminalTextUtils.getColumnWidth(segments.get(i).getText());
		}
		if(widthLimit != Integer.MAX_VALUE && width > widthLimit) {
			width = widthLimit;
		}
		return new TerminalSize(Math.max(0, width), 1);
	}

	private final class SegmentRenderer implements ComponentRenderer<TuiStatusSegmentLabel> {
		@Override
		public TerminalSize getPreferredSize(TuiStatusSegmentLabel component) {
			return component.calculatePreferredSize();
		}

		@Override
		public void drawComponent(TextGUIGraphics graphics, TuiStatusSegmentLabel component) {
			ThemeDefinition themeDefinition = component.getThemeDefinition();
			int maxCols = graphics.getSize().getColumns();
			if(component.maxColumns != Integer.MAX_VALUE && component.maxColumns < maxCols) {
				maxCols = component.maxColumns;
			}
			if(component.labelWidth != null && component.labelWidth.intValue() < maxCols) {
				maxCols = component.labelWidth.intValue();
			}
			int col = 0;
			for(int i = 0; i < component.segments.size() && col < maxCols; i++) {
				Segment segment = component.segments.get(i);
				String text = segment.getText();
				if(text == null || text.length() == 0) {
					continue;
				}
				int remaining = maxCols - col;
				if(remaining <= 0) {
					break;
				}
				String fitted = TerminalTextUtils.fitString(text, remaining);
				if(segment.getForeground() != null) {
					graphics.setForegroundColor(segment.getForeground());
					graphics.applyThemeStyle(themeDefinition.getNormal());
					graphics.setForegroundColor(segment.getForeground());
				} else {
					graphics.applyThemeStyle(themeDefinition.getNormal());
				}
				graphics.putString(col, 0, fitted);
				col += TerminalTextUtils.getColumnWidth(fitted);
			}
			while(col < maxCols) {
				graphics.applyThemeStyle(themeDefinition.getNormal());
				graphics.putString(col, 0, " ");
				col++;
			}
		}
	}
}
