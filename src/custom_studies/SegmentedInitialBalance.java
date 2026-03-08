package custom_studies;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.study.*;

import java.util.*;

@StudyHeader(
    namespace="custom", id="SEGMENTED_IB", label="Segmented IB", 
    name="Segmented Initial Balance", desc="IB lines from start to next start, max historical, +1.5x/2x ext",
    menu="Custom", overlay=true, studyOverlay=true
)
public class SegmentedInitialBalance extends Study 
{
    enum Vals { IBH, IBL } // per bar values if needed

    private List<IBRange> historicalRanges = new ArrayList<>(); // store past IBs
    private IBRange currentRange = null;

    private int lastDayKey = -1;

    @Override
    public void initialize(Defaults defaults) 
    {
        var sd = createSD();
        var tab = sd.addTab("General");

        var sessGroup = tab.addGroup("Session");
        // Use TimeFrameDescriptor for session start time
        sessGroup.addRow(new StringDescriptor("IB_START", "IB Start Time (HHMMSS)", "093000"));
        sessGroup.addRow(new StringDescriptor("IB_END", "IB End Time (HHMMSS)", "103000"));
        sessGroup.addRow(new StringDescriptor("TZ", "Timezone", "America/New_York"));

        var dispGroup = tab.addGroup("Display");
        dispGroup.addRow(new IntegerDescriptor("MAX_PRINTS", "Max Historical Prints", 5, 1, 20, 1));
        dispGroup.addRow(new BooleanDescriptor("SHOW_1_5X", "Show 1.5x Extensions", false));
        dispGroup.addRow(new BooleanDescriptor("SHOW_2X", "Show 2.0x Extensions", true));

        var rt = createRD();
        // We won't use series paths for segmentation; use onRender for precise control
    }

    @Override
    protected void calculate(int index, DataContext ctx) 
    {
        if (index < 1) return;

        DataSeries series = ctx.getDataSeries();
        long time = series.getStartTime(index);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int dayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR);

        String ibStart = getSettings().getString("IB_START", "093000");
        String ibEnd = getSettings().getString("IB_END", "103000");
        
        int currentTime = cal.get(Calendar.HOUR_OF_DAY) * 10000 + 
                         cal.get(Calendar.MINUTE) * 100 + 
                         cal.get(Calendar.SECOND);
        
        int startInt = Integer.parseInt(ibStart);
        int endInt = Integer.parseInt(ibEnd);

        boolean inSess = currentTime >= startInt && currentTime < endInt;

        if (dayKey != lastDayKey) {
            // New day: finalize previous if open, start fresh
            if (currentRange != null && currentRange.endIndex == -1) {
                currentRange.endIndex = index - 1; // previous bar
                historicalRanges.add(0, currentRange); // add to front
            }
            currentRange = new IBRange(index, dayKey);
            lastDayKey = dayKey;
        }

        if (currentRange != null) {
            if (inSess) {
                currentRange.update(series.getHigh(index), series.getLow(index));
            } else if (currentRange.startIndex < index && currentRange.endIndex == -1) {
                // Session ended this bar
                currentRange.endIndex = index - 1;
                historicalRanges.add(0, currentRange);
                currentRange = null; // wait for next day
            }
        }

        // Limit history
        int max = getSettings().getInteger("MAX_PRINTS", 5);
        while (historicalRanges.size() > max) {
            historicalRanges.remove(historicalRanges.size() - 1);
        }
    }

    /*
    @Override
    public void onRender(Graphics2D g, DataContext ctx) 
    {
        if (ctx.getDataSeries().size() < 2) return;

        DataSeries series = ctx.getDataSeries();
        int lastIdx = series.size() - 1;

        // Draw historical (completed) ranges
        for (IBRange r : historicalRanges) {
            if (r.high == Double.NaN) continue;
            drawRange(g, ctx, r.startIndex, r.endIndex == -1 ? lastIdx : r.endIndex, r.high, r.low, true);
        }

        // Draw current (if started)
        if (currentRange != null && currentRange.high != Double.NaN) {
            drawRange(g, ctx, currentRange.startIndex, lastIdx, currentRange.high, currentRange.low, false);
        }
    }

    private void drawRange(Graphics2D g, DataContext ctx, int startIdx, int endIdx, double h, double l, boolean historical) 
    {
        if (startIdx > endIdx) return;

        Coordinate left = ctx.translateBarIndex(startIdx, (h + l)/2); // dummy y
        Coordinate right = ctx.translateBarIndex(endIdx, (h + l)/2);

        int x1 = left.x;
        int x2 = right.x;

        // Main High line
        Coordinate hLeft = ctx.translateBarIndex(startIdx, h);
        Coordinate hRight = ctx.translateBarIndex(endIdx, h);
        g.setColor(getSettings().getColor("COL_HIGH"));
        g.drawLine(hLeft.x, hLeft.y, hRight.x, hRight.y);

        // Main Low line
        Coordinate lLeft = ctx.translateBarIndex(startIdx, l);
        Coordinate lRight = ctx.translateBarIndex(endIdx, l);
        g.setColor(getSettings().getColor("COL_LOW"));
        g.drawLine(lLeft.x, lLeft.y, lRight.x, lRight.y);

        double range = h - l;

        if (getSettings().getBoolean("SHOW_1_5X")) {
            double extH = h + range * 0.5;
            double extL = l - range * 0.5;
            g.setColor(getSettings().getColor("COL_1_5X"));
            Coordinate ehL = ctx.translateBarIndex(startIdx, extH);
            Coordinate ehR = ctx.translateBarIndex(endIdx, extH);
            g.drawLine(ehL.x, ehL.y, ehR.x, ehR.y);
            Coordinate elL = ctx.translateBarIndex(startIdx, extL);
            Coordinate elR = ctx.translateBarIndex(endIdx, extL);
            g.drawLine(elL.x, elL.y, elR.x, elR.y);
        }

        if (getSettings().getBoolean("SHOW_2X")) {
            double extH = h + range;
            double extL = l - range;
            g.setColor(getSettings().getColor("COL_2X"));
            // similar drawLine for extH and extL
            Coordinate ehL = ctx.translateBarIndex(startIdx, extH);
            Coordinate ehR = ctx.translateBarIndex(endIdx, extH);
            g.drawLine(ehL.x, ehL.y, ehR.x, ehR.y);
            Coordinate elL = ctx.translateBarIndex(startIdx, extL);
            Coordinate elR = ctx.translateBarIndex(endIdx, extL);
            g.drawLine(elL.x, elL.y, elR.x, elR.y);
        }
    }
    */

    private static class IBRange 
    {
        int startIndex;
        int endIndex = -1;
        double high = Double.NaN;
        double low = Double.NaN;
        int dayKey;

        IBRange(int start, int dk) { startIndex = start; dayKey = dk; }

        void update(double h, double l) 
        {
            if (Double.isNaN(high)) {
                high = h; low = l;
            } else {
                high = Math.max(high, h);
                low = Math.min(low, l);
            }
        }
    }
}
