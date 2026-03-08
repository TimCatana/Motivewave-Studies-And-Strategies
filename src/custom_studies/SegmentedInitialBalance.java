package custom_studies;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.study.*;
import java.util.*;

@StudyHeader(
    namespace="custom", id="SEGMENTED_IB", label="Segmented IB", 
    name="Segmented Initial Balance",
    desc="TradingView-style Initial Balance with optional extensions",
    menu="Custom", overlay=true
)
public class SegmentedInitialBalance extends Study 
{
    enum Values {
        IB_HIGH,
        IB_LOW,
        IB_MID,
        EXT15_HIGH,
        EXT15_LOW,
        EXT20_HIGH,
        EXT20_LOW
    }

    private static final String IB_START = "ibStart";
    private static final String IB_END = "ibEnd";
    private static final String TZ = "tz";

    private static final String PATH_IB_HIGH = "pathIbHigh";
    private static final String PATH_IB_LOW = "pathIbLow";
    private static final String PATH_IB_MID = "pathIbMid";
    private static final String PATH_15_HIGH = "path15High";
    private static final String PATH_15_LOW = "path15Low";
    private static final String PATH_20_HIGH = "path20High";
    private static final String PATH_20_LOW = "path20Low";

    private int activeSegmentKey = Integer.MIN_VALUE;
    private int segmentStartIndex = -1;
    private double ibHigh = Double.NaN;
    private double ibLow = Double.NaN;
    private boolean started = false;

    @Override
    public void initialize(Defaults defaults) 
    {
        var sd = createSD();
        var tab = sd.addTab("General");

        var sessGroup = tab.addGroup("Session");
        sessGroup.addRow(new IntegerDescriptor(IB_START, "IB Start (HHMM)", 930, 0, 2359, 1));
        sessGroup.addRow(new IntegerDescriptor(IB_END, "IB End (HHMM)", 1030, 0, 2359, 1));
        sessGroup.addRow(new StringDescriptor(TZ, "Timezone", "America/New_York"));

        tab = sd.addTab("Display");
        var ibGroup = tab.addGroup("IB Lines");
        ibGroup.addRow(new PathDescriptor(PATH_IB_HIGH, "IB High", defaults.getBlue(), 2.0f, null, true, true, true));
        ibGroup.addRow(new PathDescriptor(PATH_IB_LOW, "IB Low", defaults.getBlue(), 2.0f, null, true, true, true));
        ibGroup.addRow(new PathDescriptor(PATH_IB_MID, "IB Mid", defaults.getYellow(), 1.5f, null, true, true, true));

        var x15Group = tab.addGroup("1.5x Extensions");
        x15Group.addRow(new PathDescriptor(PATH_15_HIGH, "1.5x High", defaults.getGrey(), 1.0f, null, true, true, true));
        x15Group.addRow(new PathDescriptor(PATH_15_LOW, "1.5x Low", defaults.getGrey(), 1.0f, null, true, true, true));

        var x20Group = tab.addGroup("2.0x Extensions");
        x20Group.addRow(new PathDescriptor(PATH_20_HIGH, "2.0x High", defaults.getRed(), 1.0f, null, true, true, true));
        x20Group.addRow(new PathDescriptor(PATH_20_LOW, "2.0x Low", defaults.getRed(), 1.0f, null, true, true, true));

        var desc = createRD();
        desc.exportValue(new ValueDescriptor(Values.IB_HIGH, "IB High", new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.IB_LOW, "IB Low", new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.IB_MID, "IB Mid", new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.EXT15_HIGH, "IB 1.5x High", new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.EXT15_LOW, "IB 1.5x Low", new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.EXT20_HIGH, "IB 2.0x High", new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.EXT20_LOW, "IB 2.0x Low", new String[]{}));

        desc.declarePath(Values.IB_HIGH, PATH_IB_HIGH);
        desc.declarePath(Values.IB_LOW, PATH_IB_LOW);
        desc.declarePath(Values.IB_MID, PATH_IB_MID);
        desc.declarePath(Values.EXT15_HIGH, PATH_15_HIGH);
        desc.declarePath(Values.EXT15_LOW, PATH_15_LOW);
        desc.declarePath(Values.EXT20_HIGH, PATH_20_HIGH);
        desc.declarePath(Values.EXT20_LOW, PATH_20_LOW);
        desc.setRangeKeys(Values.IB_HIGH, Values.IB_LOW, Values.IB_MID, Values.EXT15_HIGH, Values.EXT15_LOW, Values.EXT20_HIGH, Values.EXT20_LOW);
    }

    @Override
    protected void calculate(int index, DataContext ctx) 
    {
        if (index < 0) return;

        var series = ctx.getDataSeries();
        long barTime = series.getStartTime(index);

        int start = clampHHMM(getSettings().getInteger(IB_START, 930));
        int end = clampHHMM(getSettings().getInteger(IB_END, 1030));
        TimeZone tz = TimeZone.getTimeZone(getSettings().getString(TZ, "America/New_York"));

        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(barTime);

        int hhmm = cal.get(Calendar.HOUR_OF_DAY) * 100 + cal.get(Calendar.MINUTE);
        boolean inSession = hhmm >= start && hhmm < end;
        int segmentKey = getSegmentKey(cal, start);
        boolean startsNewSegment = (segmentKey != activeSegmentKey) && (hhmm >= start);

        if (startsNewSegment) {
            // Force a visual break so MotiveWave does not connect old/new segments with a vertical line.
            if (index > 0) clearValuesAt(series, index - 1);
            activeSegmentKey = segmentKey;
            segmentStartIndex = index;
            ibHigh = Double.NaN;
            ibLow = Double.NaN;
            started = false;
        }

        if (inSession && segmentKey == activeSegmentKey) {
            double h = series.getHigh(index);
            double l = series.getLow(index);
            if (!started) {
                ibHigh = h;
                ibLow = l;
                started = true;
            }
            else {
                ibHigh = Math.max(ibHigh, h);
                ibLow = Math.min(ibLow, l);
            }
        }

        if (!started || segmentStartIndex < 0) {
            series.setComplete(index);
            return;
        }

        double range = ibHigh - ibLow;
        double ibMid = (ibHigh + ibLow) / 2.0;
        double ext15High = ibHigh + (0.5 * range);
        double ext15Low = ibLow - (0.5 * range);
        double ext20High = ibHigh + range;
        double ext20Low = ibLow - range;

        if (inSession) {
            // Keep lines flat while IB is still forming by rewriting from segment start.
            for (int i = segmentStartIndex; i <= index; i++) {
                writeValuesAt(series, i, ibHigh, ibLow, ibMid, ext15High, ext15Low, ext20High, ext20Low);
                series.setComplete(i);
            }
        }
        else {
            writeValuesAt(series, index, ibHigh, ibLow, ibMid, ext15High, ext15Low, ext20High, ext20Low);
        }

        series.setComplete(index);
    }

    private void writeValuesAt(DataSeries series, int index, double high, double low, double mid,
                               double ext15High, double ext15Low, double ext20High, double ext20Low) {
        series.setDouble(index, Values.IB_HIGH, high);
        series.setDouble(index, Values.IB_LOW, low);
        series.setDouble(index, Values.IB_MID, mid);
        series.setDouble(index, Values.EXT15_HIGH, ext15High);
        series.setDouble(index, Values.EXT15_LOW, ext15Low);
        series.setDouble(index, Values.EXT20_HIGH, ext20High);
        series.setDouble(index, Values.EXT20_LOW, ext20Low);
    }

    private void clearValuesAt(DataSeries series, int index) {
        series.setDouble(index, Values.IB_HIGH, Double.NaN);
        series.setDouble(index, Values.IB_LOW, Double.NaN);
        series.setDouble(index, Values.IB_MID, Double.NaN);
        series.setDouble(index, Values.EXT15_HIGH, Double.NaN);
        series.setDouble(index, Values.EXT15_LOW, Double.NaN);
        series.setDouble(index, Values.EXT20_HIGH, Double.NaN);
        series.setDouble(index, Values.EXT20_LOW, Double.NaN);
    }

    private int clampHHMM(int value) {
        int hh = Math.max(0, Math.min(23, value / 100));
        int mm = Math.max(0, Math.min(59, value % 100));
        return hh * 100 + mm;
    }

    private int getSegmentKey(Calendar cal, int startHHMM) {
        int hhmm = cal.get(Calendar.HOUR_OF_DAY) * 100 + cal.get(Calendar.MINUTE);
        Calendar keyCal = (Calendar)cal.clone();
        if (hhmm < startHHMM) keyCal.add(Calendar.DAY_OF_YEAR, -1);
        return keyCal.get(Calendar.YEAR) * 1000 + keyCal.get(Calendar.DAY_OF_YEAR);
    }
}
