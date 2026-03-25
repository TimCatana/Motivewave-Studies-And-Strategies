package custom_studies;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.PathInfo;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.desc.StringDescriptor;
import com.motivewave.platform.sdk.common.desc.ValueDescriptor;
import com.motivewave.platform.sdk.draw.Label;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.awt.Color;
import java.awt.Font;
import java.util.Calendar;
import java.util.TimeZone;

@StudyHeader(
    namespace = "custom",
    id = "SESSION_DAY_WEEK_HL",
    label = "Session+Day+Week HL",
    name = "Session Day Week High Low",
    desc = "Asia/London/NY session highs/lows plus current/previous day and week highs/lows",
    menu = "Custom",
    overlay = true,
    studyOverlay = true
)
public class SessionDayWeekHighLow extends Study {

  enum Values {
    ASIA_CUR_HIGH,
    ASIA_CUR_LOW,
    ASIA_PREV_HIGH,
    ASIA_PREV_LOW,
    LONDON_CUR_HIGH,
    LONDON_CUR_LOW,
    LONDON_PREV_HIGH,
    LONDON_PREV_LOW,
    NY_CUR_HIGH,
    NY_CUR_LOW,
    NY_PREV_HIGH,
    NY_PREV_LOW,
    DAY_CUR_HIGH,
    DAY_CUR_LOW,
    DAY_PREV_HIGH,
    DAY_PREV_LOW,
    WEEK_CUR_HIGH,
    WEEK_CUR_LOW,
    WEEK_PREV_HIGH,
    WEEK_PREV_LOW
  }

  private static final String TZ = "timezone";

  private static final String ASIA_START = "asiaStart";
  private static final String ASIA_END = "asiaEnd";
  private static final String LONDON_START = "londonStart";
  private static final String LONDON_END = "londonEnd";
  private static final String NY_START = "nyStart";
  private static final String NY_END = "nyEnd";
  private static final String SHOW_LABELS = "showLabels";
  private static final String LABEL_OFFSET_TICKS = "labelOffsetTicks";

  private static final String SHOW_ASIA_CUR_HIGH = "showAsiaCurHigh";
  private static final String SHOW_ASIA_CUR_LOW = "showAsiaCurLow";
  private static final String SHOW_ASIA_PREV_HIGH = "showAsiaPrevHigh";
  private static final String SHOW_ASIA_PREV_LOW = "showAsiaPrevLow";
  private static final String SHOW_LONDON_CUR_HIGH = "showLondonCurHigh";
  private static final String SHOW_LONDON_CUR_LOW = "showLondonCurLow";
  private static final String SHOW_LONDON_PREV_HIGH = "showLondonPrevHigh";
  private static final String SHOW_LONDON_PREV_LOW = "showLondonPrevLow";
  private static final String SHOW_NY_CUR_HIGH = "showNyCurHigh";
  private static final String SHOW_NY_CUR_LOW = "showNyCurLow";
  private static final String SHOW_NY_PREV_HIGH = "showNyPrevHigh";
  private static final String SHOW_NY_PREV_LOW = "showNyPrevLow";
  private static final String SHOW_DAY_CUR_HIGH = "showDayCurHigh";
  private static final String SHOW_DAY_CUR_LOW = "showDayCurLow";
  private static final String SHOW_DAY_PREV_HIGH = "showDayPrevHigh";
  private static final String SHOW_DAY_PREV_LOW = "showDayPrevLow";
  private static final String SHOW_WEEK_CUR_HIGH = "showWeekCurHigh";
  private static final String SHOW_WEEK_CUR_LOW = "showWeekCurLow";
  private static final String SHOW_WEEK_PREV_HIGH = "showWeekPrevHigh";
  private static final String SHOW_WEEK_PREV_LOW = "showWeekPrevLow";

  private static final String PATH_ASIA_CUR_HIGH = "pathAsiaCurHigh";
  private static final String PATH_ASIA_CUR_LOW = "pathAsiaCurLow";
  private static final String PATH_ASIA_PREV_HIGH = "pathAsiaPrevHigh";
  private static final String PATH_ASIA_PREV_LOW = "pathAsiaPrevLow";
  private static final String PATH_LONDON_CUR_HIGH = "pathLondonCurHigh";
  private static final String PATH_LONDON_CUR_LOW = "pathLondonCurLow";
  private static final String PATH_LONDON_PREV_HIGH = "pathLondonPrevHigh";
  private static final String PATH_LONDON_PREV_LOW = "pathLondonPrevLow";
  private static final String PATH_NY_CUR_HIGH = "pathNyCurHigh";
  private static final String PATH_NY_CUR_LOW = "pathNyCurLow";
  private static final String PATH_NY_PREV_HIGH = "pathNyPrevHigh";
  private static final String PATH_NY_PREV_LOW = "pathNyPrevLow";
  private static final String PATH_DAY_CUR_HIGH = "pathDayCurHigh";
  private static final String PATH_DAY_CUR_LOW = "pathDayCurLow";
  private static final String PATH_DAY_PREV_HIGH = "pathDayPrevHigh";
  private static final String PATH_DAY_PREV_LOW = "pathDayPrevLow";
  private static final String PATH_WEEK_CUR_HIGH = "pathWeekCurHigh";
  private static final String PATH_WEEK_CUR_LOW = "pathWeekCurLow";
  private static final String PATH_WEEK_PREV_HIGH = "pathWeekPrevHigh";
  private static final String PATH_WEEK_PREV_LOW = "pathWeekPrevLow";

  private static final double NAN = Double.NaN;

  private static class RangeState {
    double currentHigh = NAN;
    double currentLow = NAN;
    double previousHigh = NAN;
    double previousLow = NAN;
    int currentHighIndex = -1;
    int currentLowIndex = -1;
    int previousHighIndex = -1;
    int previousLowIndex = -1;

    void clearCurrent() {
      currentHigh = NAN;
      currentLow = NAN;
      currentHighIndex = -1;
      currentLowIndex = -1;
    }

    void shiftCurrentToPrevious() {
      if (isCurrentValid()) {
        previousHigh = currentHigh;
        previousLow = currentLow;
        previousHighIndex = currentHighIndex;
        previousLowIndex = currentLowIndex;
      }
      else {
        previousHigh = NAN;
        previousLow = NAN;
        previousHighIndex = -1;
        previousLowIndex = -1;
      }
      clearCurrent();
    }

    void startCurrent(int index, double high, double low) {
      currentHigh = high;
      currentLow = low;
      currentHighIndex = index;
      currentLowIndex = index;
    }

    void updateCurrent(int index, double high, double low) {
      if (Double.isNaN(currentHigh) || high >= currentHigh) {
        currentHigh = high;
        currentHighIndex = index;
      }
      if (Double.isNaN(currentLow) || low <= currentLow) {
        currentLow = low;
        currentLowIndex = index;
      }
    }

    boolean isCurrentValid() {
      return !Double.isNaN(currentHigh) && !Double.isNaN(currentLow);
    }
  }

  private final RangeState asia = new RangeState();
  private final RangeState london = new RangeState();
  private final RangeState ny = new RangeState();
  private final RangeState day = new RangeState();
  private final RangeState week = new RangeState();

  private int lastIndex = -1;
  private boolean prevAsiaIn = false;
  private boolean prevLondonIn = false;
  private boolean prevNyIn = false;
  private int activeDayKey = Integer.MIN_VALUE;
  private int activeWeekKey = Integer.MIN_VALUE;

  @Override
  public void initialize(Defaults defaults) {
    var sd = createSD();

    var generalTab = sd.addTab("General");
    var sessionGroup = generalTab.addGroup("Session Windows");
    sessionGroup.addRow(new StringDescriptor(TZ, "Session Timezone", "America/New_York"));
    sessionGroup.addRow(new IntegerDescriptor(ASIA_START, "Asia Start (HHMM)", 1800, 0, 2359, 1));
    sessionGroup.addRow(new IntegerDescriptor(ASIA_END, "Asia End (HHMM)", 259, 0, 2359, 1));
    sessionGroup.addRow(new IntegerDescriptor(LONDON_START, "London Start (HHMM)", 300, 0, 2359, 1));
    sessionGroup.addRow(new IntegerDescriptor(LONDON_END, "London End (HHMM)", 929, 0, 2359, 1));
    sessionGroup.addRow(new IntegerDescriptor(NY_START, "New York Start (HHMM)", 930, 0, 2359, 1));
    sessionGroup.addRow(new IntegerDescriptor(NY_END, "New York End (HHMM)", 1859, 0, 2359, 1));
    sessionGroup.addRow(new BooleanDescriptor(SHOW_LABELS, "Show Labels", true));
    sessionGroup.addRow(new IntegerDescriptor(LABEL_OFFSET_TICKS, "Label Offset (ticks)", 2, 0, 100, 1));

    var visGroup = generalTab.addGroup("Visibility");
    visGroup.addRow(new BooleanDescriptor(SHOW_ASIA_CUR_HIGH, "Asia Current High", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_ASIA_CUR_LOW, "Asia Current Low", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_ASIA_PREV_HIGH, "Asia Previous High", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_ASIA_PREV_LOW, "Asia Previous Low", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_LONDON_CUR_HIGH, "London Current High", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_LONDON_CUR_LOW, "London Current Low", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_LONDON_PREV_HIGH, "London Previous High", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_LONDON_PREV_LOW, "London Previous Low", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_NY_CUR_HIGH, "New York Current High", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_NY_CUR_LOW, "New York Current Low", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_NY_PREV_HIGH, "New York Previous High", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_NY_PREV_LOW, "New York Previous Low", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_DAY_CUR_HIGH, "Day Current High", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_DAY_CUR_LOW, "Day Current Low", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_DAY_PREV_HIGH, "Day Previous High", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_DAY_PREV_LOW, "Day Previous Low", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_WEEK_CUR_HIGH, "Week Current High", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_WEEK_CUR_LOW, "Week Current Low", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_WEEK_PREV_HIGH, "Week Previous High", true));
    visGroup.addRow(new BooleanDescriptor(SHOW_WEEK_PREV_LOW, "Week Previous Low", true));

    var linesTab = sd.addTab("Lines");
    var asiaGroup = linesTab.addGroup("Asia");
    asiaGroup.addRow(new PathDescriptor(PATH_ASIA_CUR_HIGH, "Asia Current High", defaults.getGreen(), 2.0f, null, true, true, true));
    asiaGroup.addRow(new PathDescriptor(PATH_ASIA_CUR_LOW, "Asia Current Low", defaults.getRed(), 2.0f, null, true, true, true));
    asiaGroup.addRow(new PathDescriptor(PATH_ASIA_PREV_HIGH, "Asia Previous High", defaults.getGreen(), 1.0f, null, true, true, true));
    asiaGroup.addRow(new PathDescriptor(PATH_ASIA_PREV_LOW, "Asia Previous Low", defaults.getRed(), 1.0f, null, true, true, true));

    var londonGroup = linesTab.addGroup("London");
    londonGroup.addRow(new PathDescriptor(PATH_LONDON_CUR_HIGH, "London Current High", defaults.getGreen(), 2.0f, null, true, true, true));
    londonGroup.addRow(new PathDescriptor(PATH_LONDON_CUR_LOW, "London Current Low", defaults.getRed(), 2.0f, null, true, true, true));
    londonGroup.addRow(new PathDescriptor(PATH_LONDON_PREV_HIGH, "London Previous High", defaults.getGreen(), 1.0f, null, true, true, true));
    londonGroup.addRow(new PathDescriptor(PATH_LONDON_PREV_LOW, "London Previous Low", defaults.getRed(), 1.0f, null, true, true, true));

    var nyGroup = linesTab.addGroup("New York");
    nyGroup.addRow(new PathDescriptor(PATH_NY_CUR_HIGH, "New York Current High", defaults.getGreen(), 2.0f, null, true, true, true));
    nyGroup.addRow(new PathDescriptor(PATH_NY_CUR_LOW, "New York Current Low", defaults.getRed(), 2.0f, null, true, true, true));
    nyGroup.addRow(new PathDescriptor(PATH_NY_PREV_HIGH, "New York Previous High", defaults.getGreen(), 1.0f, null, true, true, true));
    nyGroup.addRow(new PathDescriptor(PATH_NY_PREV_LOW, "New York Previous Low", defaults.getRed(), 1.0f, null, true, true, true));

    var dayGroup = linesTab.addGroup("Day");
    dayGroup.addRow(new PathDescriptor(PATH_DAY_CUR_HIGH, "Day Current High", defaults.getBlue(), 2.0f, null, true, true, true));
    dayGroup.addRow(new PathDescriptor(PATH_DAY_CUR_LOW, "Day Current Low", defaults.getOrange(), 2.0f, null, true, true, true));
    dayGroup.addRow(new PathDescriptor(PATH_DAY_PREV_HIGH, "Day Previous High", defaults.getBlue(), 1.0f, null, true, true, true));
    dayGroup.addRow(new PathDescriptor(PATH_DAY_PREV_LOW, "Day Previous Low", defaults.getOrange(), 1.0f, null, true, true, true));

    var weekGroup = linesTab.addGroup("Week");
    weekGroup.addRow(new PathDescriptor(PATH_WEEK_CUR_HIGH, "Week Current High", defaults.getBlue(), 2.0f, null, true, true, true));
    weekGroup.addRow(new PathDescriptor(PATH_WEEK_CUR_LOW, "Week Current Low", defaults.getPurple(), 2.0f, null, true, true, true));
    weekGroup.addRow(new PathDescriptor(PATH_WEEK_PREV_HIGH, "Week Previous High", defaults.getBlue(), 1.0f, null, true, true, true));
    weekGroup.addRow(new PathDescriptor(PATH_WEEK_PREV_LOW, "Week Previous Low", defaults.getPurple(), 1.0f, null, true, true, true));

    var desc = createRD();
    desc.exportValue(new ValueDescriptor(Values.ASIA_CUR_HIGH, "Asia Current High", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.ASIA_CUR_LOW, "Asia Current Low", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.ASIA_PREV_HIGH, "Asia Previous High", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.ASIA_PREV_LOW, "Asia Previous Low", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.LONDON_CUR_HIGH, "London Current High", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.LONDON_CUR_LOW, "London Current Low", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.LONDON_PREV_HIGH, "London Previous High", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.LONDON_PREV_LOW, "London Previous Low", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.NY_CUR_HIGH, "New York Current High", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.NY_CUR_LOW, "New York Current Low", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.NY_PREV_HIGH, "New York Previous High", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.NY_PREV_LOW, "New York Previous Low", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.DAY_CUR_HIGH, "Day Current High", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.DAY_CUR_LOW, "Day Current Low", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.DAY_PREV_HIGH, "Day Previous High", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.DAY_PREV_LOW, "Day Previous Low", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.WEEK_CUR_HIGH, "Week Current High", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.WEEK_CUR_LOW, "Week Current Low", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.WEEK_PREV_HIGH, "Week Previous High", new String[]{}));
    desc.exportValue(new ValueDescriptor(Values.WEEK_PREV_LOW, "Week Previous Low", new String[]{}));

    desc.setRangeKeys(
        Values.ASIA_CUR_HIGH, Values.ASIA_CUR_LOW,
        Values.ASIA_PREV_HIGH, Values.ASIA_PREV_LOW,
        Values.LONDON_CUR_HIGH, Values.LONDON_CUR_LOW,
        Values.LONDON_PREV_HIGH, Values.LONDON_PREV_LOW,
        Values.NY_CUR_HIGH, Values.NY_CUR_LOW,
        Values.NY_PREV_HIGH, Values.NY_PREV_LOW,
        Values.DAY_CUR_HIGH, Values.DAY_CUR_LOW,
        Values.DAY_PREV_HIGH, Values.DAY_PREV_LOW,
        Values.WEEK_CUR_HIGH, Values.WEEK_CUR_LOW,
        Values.WEEK_PREV_HIGH, Values.WEEK_PREV_LOW
    );
  }

  @Override
  protected void calculate(int index, DataContext ctx) {
    if (index < 0) return;

    DataSeries series = ctx.getDataSeries();
    if (index == 0 || (lastIndex >= 0 && index < lastIndex)) {
      resetState();
    }

    TimeZone tz = TimeZone.getTimeZone(getSettings().getString(TZ, "America/New_York"));

    int asiaStart = clampHHMM(getSettings().getInteger(ASIA_START, 1800));
    int asiaEnd = clampHHMM(getSettings().getInteger(ASIA_END, 259));
    int londonStart = clampHHMM(getSettings().getInteger(LONDON_START, 300));
    int londonEnd = clampHHMM(getSettings().getInteger(LONDON_END, 929));
    int nyStart = clampHHMM(getSettings().getInteger(NY_START, 930));
    int nyEnd = clampHHMM(getSettings().getInteger(NY_END, 1859));

    long barTime = series.getStartTime(index);
    Calendar cal = Calendar.getInstance(tz);
    cal.setTimeInMillis(barTime);

    int hhmm = toHHMM(cal);
    boolean asiaIn = inSession(hhmm, asiaStart, asiaEnd);
    boolean londonIn = inSession(hhmm, londonStart, londonEnd);
    boolean nyIn = inSession(hhmm, nyStart, nyEnd);

    double high = series.getHigh(index);
    double low = series.getLow(index);

    if (asiaIn && !prevAsiaIn) {
      breakLinesAt(series, index - 1, Values.ASIA_CUR_HIGH, Values.ASIA_CUR_LOW, Values.ASIA_PREV_HIGH, Values.ASIA_PREV_LOW);
      asia.shiftCurrentToPrevious();
      asia.startCurrent(index, high, low);
    } else if (asiaIn) {
      asia.updateCurrent(index, high, low);
    }

    if (londonIn && !prevLondonIn) {
      breakLinesAt(series, index - 1, Values.LONDON_CUR_HIGH, Values.LONDON_CUR_LOW, Values.LONDON_PREV_HIGH, Values.LONDON_PREV_LOW);
      london.shiftCurrentToPrevious();
      london.startCurrent(index, high, low);
    } else if (londonIn) {
      london.updateCurrent(index, high, low);
    }

    if (nyIn && !prevNyIn) {
      breakLinesAt(series, index - 1, Values.NY_CUR_HIGH, Values.NY_CUR_LOW, Values.NY_PREV_HIGH, Values.NY_PREV_LOW);
      ny.shiftCurrentToPrevious();
      ny.startCurrent(index, high, low);
    } else if (nyIn) {
      ny.updateCurrent(index, high, low);
    }

    int dayKey = getDayKey(cal);
    if (activeDayKey == Integer.MIN_VALUE) {
      activeDayKey = dayKey;
      day.startCurrent(index, high, low);
    } else if (dayKey != activeDayKey) {
      breakLinesAt(series, index - 1, Values.DAY_CUR_HIGH, Values.DAY_CUR_LOW, Values.DAY_PREV_HIGH, Values.DAY_PREV_LOW);
      day.shiftCurrentToPrevious();
      day.startCurrent(index, high, low);
      activeDayKey = dayKey;
    } else {
      day.updateCurrent(index, high, low);
    }

    int weekKey = getWeekKey(cal);
    if (activeWeekKey == Integer.MIN_VALUE) {
      activeWeekKey = weekKey;
      week.startCurrent(index, high, low);
    } else if (weekKey != activeWeekKey) {
      breakLinesAt(series, index - 1, Values.WEEK_CUR_HIGH, Values.WEEK_CUR_LOW, Values.WEEK_PREV_HIGH, Values.WEEK_PREV_LOW);
      week.shiftCurrentToPrevious();
      week.startCurrent(index, high, low);
      activeWeekKey = weekKey;
    } else {
      week.updateCurrent(index, high, low);
    }

    writeValue(series, index, Values.ASIA_CUR_HIGH, SHOW_ASIA_CUR_HIGH, asia.currentHigh);
    writeValue(series, index, Values.ASIA_CUR_LOW, SHOW_ASIA_CUR_LOW, asia.currentLow);
    writeValue(series, index, Values.ASIA_PREV_HIGH, SHOW_ASIA_PREV_HIGH, asia.previousHigh);
    writeValue(series, index, Values.ASIA_PREV_LOW, SHOW_ASIA_PREV_LOW, asia.previousLow);

    writeValue(series, index, Values.LONDON_CUR_HIGH, SHOW_LONDON_CUR_HIGH, london.currentHigh);
    writeValue(series, index, Values.LONDON_CUR_LOW, SHOW_LONDON_CUR_LOW, london.currentLow);
    writeValue(series, index, Values.LONDON_PREV_HIGH, SHOW_LONDON_PREV_HIGH, london.previousHigh);
    writeValue(series, index, Values.LONDON_PREV_LOW, SHOW_LONDON_PREV_LOW, london.previousLow);

    writeValue(series, index, Values.NY_CUR_HIGH, SHOW_NY_CUR_HIGH, ny.currentHigh);
    writeValue(series, index, Values.NY_CUR_LOW, SHOW_NY_CUR_LOW, ny.currentLow);
    writeValue(series, index, Values.NY_PREV_HIGH, SHOW_NY_PREV_HIGH, ny.previousHigh);
    writeValue(series, index, Values.NY_PREV_LOW, SHOW_NY_PREV_LOW, ny.previousLow);

    writeValue(series, index, Values.DAY_CUR_HIGH, SHOW_DAY_CUR_HIGH, day.currentHigh);
    writeValue(series, index, Values.DAY_CUR_LOW, SHOW_DAY_CUR_LOW, day.currentLow);
    writeValue(series, index, Values.DAY_PREV_HIGH, SHOW_DAY_PREV_HIGH, day.previousHigh);
    writeValue(series, index, Values.DAY_PREV_LOW, SHOW_DAY_PREV_LOW, day.previousLow);

    writeValue(series, index, Values.WEEK_CUR_HIGH, SHOW_WEEK_CUR_HIGH, week.currentHigh);
    writeValue(series, index, Values.WEEK_CUR_LOW, SHOW_WEEK_CUR_LOW, week.currentLow);
    writeValue(series, index, Values.WEEK_PREV_HIGH, SHOW_WEEK_PREV_HIGH, week.previousHigh);
    writeValue(series, index, Values.WEEK_PREV_LOW, SHOW_WEEK_PREV_LOW, week.previousLow);

    prevAsiaIn = asiaIn;
    prevLondonIn = londonIn;
    prevNyIn = nyIn;
    lastIndex = index;

    renderFigures(index, ctx);

    series.setComplete(index);
  }

  private void renderFigures(int index, DataContext ctx) {
    DataSeries series = ctx.getDataSeries();
    int endIndex = Math.max(series.getEndIndex(), series.size() - 1);
    if (endIndex < 0 || index < endIndex) return;

    beginFigureUpdate();
    clearFigures();

    long endTime = series.getEndTime(endIndex);
    boolean showLabels = getSettings().getBoolean(SHOW_LABELS, true);
    int offsetTicks = Math.max(0, getSettings().getInteger(LABEL_OFFSET_TICKS, 2));
    double tickSize = 0.25;
    if (ctx.getInstrument() != null) {
      tickSize = Math.max(0.00000001, ctx.getInstrument().getTickSize());
    }
    else if (series.getInstrument() != null) {
      tickSize = Math.max(0.00000001, series.getInstrument().getTickSize());
    }
    double labelPad = offsetTicks * tickSize;

    drawLevel(series, endTime, showLabels, labelPad, SHOW_ASIA_CUR_HIGH, PATH_ASIA_CUR_HIGH, asia.currentHigh, asia.currentHighIndex, "Asia H");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_ASIA_CUR_LOW, PATH_ASIA_CUR_LOW, asia.currentLow, asia.currentLowIndex, "Asia L");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_ASIA_PREV_HIGH, PATH_ASIA_PREV_HIGH, asia.previousHigh, asia.previousHighIndex, "Prev Asia H");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_ASIA_PREV_LOW, PATH_ASIA_PREV_LOW, asia.previousLow, asia.previousLowIndex, "Prev Asia L");

    drawLevel(series, endTime, showLabels, labelPad, SHOW_LONDON_CUR_HIGH, PATH_LONDON_CUR_HIGH, london.currentHigh, london.currentHighIndex, "London H");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_LONDON_CUR_LOW, PATH_LONDON_CUR_LOW, london.currentLow, london.currentLowIndex, "London L");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_LONDON_PREV_HIGH, PATH_LONDON_PREV_HIGH, london.previousHigh, london.previousHighIndex, "Prev London H");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_LONDON_PREV_LOW, PATH_LONDON_PREV_LOW, london.previousLow, london.previousLowIndex, "Prev London L");

    drawLevel(series, endTime, showLabels, labelPad, SHOW_NY_CUR_HIGH, PATH_NY_CUR_HIGH, ny.currentHigh, ny.currentHighIndex, "New York H");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_NY_CUR_LOW, PATH_NY_CUR_LOW, ny.currentLow, ny.currentLowIndex, "New York L");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_NY_PREV_HIGH, PATH_NY_PREV_HIGH, ny.previousHigh, ny.previousHighIndex, "Prev NY H");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_NY_PREV_LOW, PATH_NY_PREV_LOW, ny.previousLow, ny.previousLowIndex, "Prev NY L");

    drawLevel(series, endTime, showLabels, labelPad, SHOW_DAY_CUR_HIGH, PATH_DAY_CUR_HIGH, day.currentHigh, day.currentHighIndex, "Day H");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_DAY_CUR_LOW, PATH_DAY_CUR_LOW, day.currentLow, day.currentLowIndex, "Day L");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_DAY_PREV_HIGH, PATH_DAY_PREV_HIGH, day.previousHigh, day.previousHighIndex, "Prev Day H");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_DAY_PREV_LOW, PATH_DAY_PREV_LOW, day.previousLow, day.previousLowIndex, "Prev Day L");

    drawLevel(series, endTime, showLabels, labelPad, SHOW_WEEK_CUR_HIGH, PATH_WEEK_CUR_HIGH, week.currentHigh, week.currentHighIndex, "Week H");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_WEEK_CUR_LOW, PATH_WEEK_CUR_LOW, week.currentLow, week.currentLowIndex, "Week L");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_WEEK_PREV_HIGH, PATH_WEEK_PREV_HIGH, week.previousHigh, week.previousHighIndex, "Prev Week H");
    drawLevel(series, endTime, showLabels, labelPad, SHOW_WEEK_PREV_LOW, PATH_WEEK_PREV_LOW, week.previousLow, week.previousLowIndex, "Prev Week L");

    endFigureUpdate();
  }

  private void drawLevel(DataSeries series, long endTime, boolean showLabels, double labelPad,
                         String showKey, String pathKey, double value, int startIndex, String labelText) {
    if (!getSettings().getBoolean(showKey, true)) return;
    if (Double.isNaN(value) || startIndex < 0) return;

    PathInfo path = getSettings().getPath(pathKey);
    if (path == null) {
      path = new PathInfo(Color.GRAY, 1.0f, null, true, true, true, 0, null);
    }

    int clampedStart = Math.max(series.getStartIndex(), Math.min(startIndex, series.getEndIndex()));
    long startTime = series.getStartTime(clampedStart);

    Line line = new Line(new Coordinate(startTime, value), new Coordinate(endTime, value), path);
    line.setExtendRight(1);
    addFigure(line);

    if (!showLabels) return;
    Label label = new Label(labelText, new Font("Dialog", Font.PLAIN, 10), path.getColor(), null);
    label.setLocation(endTime, value + labelPad);
    label.setShowLine(false);
    addFigure(label);
  }

  private void writeValue(DataSeries series, int index, Values valueKey, String showKey, double value) {
    boolean show = getSettings().getBoolean(showKey, true);
    series.setDouble(index, valueKey, show ? value : NAN);
  }

  private void breakLinesAt(DataSeries series, int index, Values... keys) {
    if (index < 0) return;
    for (Values key : keys) {
      series.setDouble(index, key, NAN);
    }
  }

  private void resetState() {
    asia.clearCurrent();
    asia.previousHigh = NAN;
    asia.previousLow = NAN;
    asia.previousHighIndex = -1;
    asia.previousLowIndex = -1;

    london.clearCurrent();
    london.previousHigh = NAN;
    london.previousLow = NAN;
    london.previousHighIndex = -1;
    london.previousLowIndex = -1;

    ny.clearCurrent();
    ny.previousHigh = NAN;
    ny.previousLow = NAN;
    ny.previousHighIndex = -1;
    ny.previousLowIndex = -1;

    day.clearCurrent();
    day.previousHigh = NAN;
    day.previousLow = NAN;
    day.previousHighIndex = -1;
    day.previousLowIndex = -1;

    week.clearCurrent();
    week.previousHigh = NAN;
    week.previousLow = NAN;
    week.previousHighIndex = -1;
    week.previousLowIndex = -1;

    lastIndex = -1;
    prevAsiaIn = false;
    prevLondonIn = false;
    prevNyIn = false;
    activeDayKey = Integer.MIN_VALUE;
    activeWeekKey = Integer.MIN_VALUE;
  }

  private int clampHHMM(int value) {
    int hh = Math.max(0, Math.min(23, value / 100));
    int mm = Math.max(0, Math.min(59, value % 100));
    return hh * 100 + mm;
  }

  private int toHHMM(Calendar cal) {
    return cal.get(Calendar.HOUR_OF_DAY) * 100 + cal.get(Calendar.MINUTE);
  }

  private boolean inSession(int hhmm, int start, int end) {
    if (start <= end) {
      return hhmm >= start && hhmm <= end;
    }
    return hhmm >= start || hhmm <= end;
  }

  private int getDayKey(Calendar cal) {
    return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR);
  }

  private int getWeekKey(Calendar cal) {
    return cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR);
  }
}
