/**
 * Link to MotiveWave Study Development Documentation: https://www.motivewave.com/sdk/javadoc/com/motivewave/platform/sdk/study/Study.html
 * Link to MotiveWave Study Attributes: https://www.motivewave.com/sdk/javadoc/com/motivewave/platform/sdk/study/StudyHeader.html
 * @Note Strategies are seperate per instrument. Thus any state reset are local to the instrument's instance not all instruments.  
*/

package custom_strategies;

import java.util.List;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.Enums.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.order_mgmt.*;
import com.motivewave.platform.sdk.study.*;

/**
 * BRACKET QUANTITY AVERAGER
 * 
 * Attaches a single bracket order (stop loss + take profit) when activated if a
 * position exists.
 * On any fill (partial close or addition), cancels the existing bracket and
 * recreates it with the updated position quantity while keeping fixed SL/TP
 * On position close, resets tracking state.
 * Supports both long and short positions.
 */
@StudyHeader(namespace = "custom_strategies", id = "BRACKET_AVERAGER", name = "Bracket Position Averager", label = "Bracket Pos Avg", desc = "Auto updates bracket quantity on position changes with fixed SL/TP prices. Basically just does normal position auto averaging", menu = "Position Averaging Strategies", overlay = true, studyOverlay = true, strategy = true, signals = false)
public class BracketAverager extends Study {

    private String stopOrderId = null;
    private String tpOrderId = null;
    private int lastKnownQty = 0;

    private float fixedSlPrice = 0;
    private float fixedTpPrice = 0;

    private static final String KEY_SL_OFFSET_TICKS = "slOffsetTicks";
    private static final String KEY_TP_OFFSET_TICKS = "tpOffsetTicks";

    /**
     * Initializes the study settings and defines the user-configurable parameters.
     * 
     * @param defaults The default settings object.
     */
    @Override
    public void initialize(Defaults defaults) {
        SettingsDescriptor sd = new SettingsDescriptor();
        setSettingsDescriptor(sd);

        SettingTab tab = new SettingTab("General");
        sd.addTab(tab);

        SettingGroup group = new SettingGroup("Bracket Offsets (ticks from avg entry)");
        tab.addGroup(group);

        // Key, Label, Default, Min, Max, Step
        group.addRow(new IntegerDescriptor(KEY_SL_OFFSET_TICKS, "Stop Loss Offset", 10, 1, 1000, 1));
        group.addRow(new IntegerDescriptor(KEY_TP_OFFSET_TICKS, "Take Profit Offset", 20, 1, 1000, 1));
    }

    /**
     * Called when the strategy is activated by the user.
     * Resets tracking state and attaches a bracket if a position is already open.
     * 
     * @param ctx The order context.
     */
    @Override
    public void onActivate(OrderContext ctx) {
        Instrument instr = ctx.getInstrument();
        if (instr == null) {
            this.warning("No instrument loaded - strategy ACTIVATED but inactive until a symbol is available.");
            return;
        }

        int posQty = ctx.getAccountPosition(instr);
        String symbol = instr.getSymbol();
        String direction = (posQty > 0) ? "Long" : (posQty < 0 ? "Short" : "Flat (No Current Position)");

        this.info("BracketAverager ACTIVATED on " + symbol + " (" + direction + ")");

        if (hasExistingBracket(ctx)) {
            this.warning("Existing bracket orders detected! This will create duplicate brackets\n\n" +
                    "If you don't want duplicates:\n" +
                    "1. Turn OFF the exit strategy power button in Trade Panel/DOM\n" +
                    "2. Manually cancel any existing SL/TP orders\n\n" +
                    "Bracket attachment currently skipped to prevent duplicates.");
        }

        resetTracking();
        attachBracketIfNeeded(ctx);
    }

    /**
     * Called when the strategy is deactivated by the user.
     * Logs deactivation and resets all tracking state.
     * 
     * @param ctx The order context.
     */
    @Override
    public void onDeactivate(OrderContext ctx) {
        this.info("BracketAverager DEACTIVATED");
        resetTracking();
    }

    /**
     * Called on every order fill (full or partial).
     * Checks if position quantity changed and triggers bracket update if necessary.
     * 
     * @param ctx   The order context.
     * @param order The filled order.
     */
    @Override
    public void onOrderFilled(OrderContext ctx, Order order) {
        Instrument instr = ctx.getInstrument();
        if (instr == null) {
            return;
        }

        int currentQty = ctx.getAccountPosition(instr);

        this.info("Order filled: " + order.getOrderId() +
                " | Filled qty: " + order.getFilled() +
                " | Position now: " + currentQty +
                " (previous: " + lastKnownQty + ")");

        if (currentQty != lastKnownQty) {
            this.info("Qty changed - updating bracket");
            updateBracketQuantities(ctx, currentQty);
        } else {
            this.info("Qty unchanged - no update needed");
        }
    }

    /**
     * Called when the position is fully closed (quantity reaches zero).
     * Resets tracking state to prepare for potential new trades.
     * 
     * @param ctx The order context.
     * @note The Motivewave SDK creates separate instances of this strategy (this is
     *       build in) per instrument so only resets for that instrument.
     *       Not globally for all instruments.
     */
    @Override
    public void onPositionClosed(OrderContext ctx) {
        this.info("Position closed - resetting bracket tracking");
        resetTracking();
    }

    // ────────────────────────────────────────────────
    // Helper Methods
    // ────────────────────────────────────────────────

    /**
     * Checks if any bracket-like orders (STOP or LIMIT on exit side) already exist.
     * 
     * @param ctx The order context.
     * @return true if existing bracket orders are detected.
     */
    private boolean hasExistingBracket(OrderContext ctx) {
        Instrument instr = ctx.getInstrument();
        if (instr == null) {
            return false;
        }

        int posQty = ctx.getAccountPosition(instr);
        OrderAction expectedAction = (posQty > 0) ? OrderAction.SELL : OrderAction.BUY;

        List<Order> active = ctx.getActiveOrders();
        for (Order o : active) {
            if (o.getAction() == expectedAction &&
                    (o.getType() == Enums.OrderType.STOP || o.getType() == Enums.OrderType.LIMIT)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resets all internal tracking state.
     * Clears order IDs, last known quantity, and fixed SL/TP prices.
     */
    private void resetTracking() {
        stopOrderId = null;
        tpOrderId = null;
        lastKnownQty = 0;
        fixedSlPrice = 0;
        fixedTpPrice = 0;
    }

    /**
     * Attaches a new bracket order if a position exists and no bracket is currently
     * tracked.
     * Calculates initial SL/TP prices from current average entry and stores them as
     * fixed.
     * Supports both long and short positions.
     * 
     * @param ctx The order context.
     */
    private void attachBracketIfNeeded(OrderContext ctx) {
        Instrument instr = ctx.getInstrument();
        if (instr == null) {
            this.info("No instrument - cannot attach bracket");
            return;
        }

        int currentQty = ctx.getAccountPosition(instr);
        if (currentQty == 0) {
            this.info("No open position - attach skipped");
            return;
        }

        if (stopOrderId != null || tpOrderId != null) {
            this.info("Bracket already tracked - attach skipped");
            return;
        }

        float avgPrice = ctx.getAvgEntryPrice(instr);
        double tickSize = instr.getTickSize();

        int slOffset = getSettings().getInteger(KEY_SL_OFFSET_TICKS);
        int tpOffset = getSettings().getInteger(KEY_TP_OFFSET_TICKS);

        float slPrice, tpPrice;
        OrderAction exitAction;
        int absQty = Math.abs(currentQty);

        if (currentQty > 0) {
            exitAction = OrderAction.SELL;
            slPrice = avgPrice - (float) (slOffset * tickSize);
            tpPrice = avgPrice + (float) (tpOffset * tickSize);
        } else {
            exitAction = OrderAction.BUY;
            slPrice = avgPrice + (float) (slOffset * tickSize);
            tpPrice = avgPrice - (float) (tpOffset * tickSize);
        }

        slPrice = instr.round(slPrice);
        tpPrice = instr.round(tpPrice);

        Order stopOrder = ctx.createStopOrder(exitAction, TIF.GTC, absQty, slPrice);
        Order tpOrder = ctx.createLimitOrder(exitAction, TIF.GTC, absQty, tpPrice);

        ctx.submitOrders(stopOrder, tpOrder);

        stopOrderId = stopOrder.getOrderId();
        tpOrderId = tpOrder.getOrderId();
        lastKnownQty = currentQty;

        fixedSlPrice = slPrice;
        fixedTpPrice = tpPrice;

        this.info("Bracket attached: SL @" + slPrice + ", TP @" + tpPrice + ", Qty=" + absQty +
                " (" + (currentQty > 0 ? "Long" : "Short") + ")");
    }

    /**
     * Updates the bracket when position quantity changes.
     * Cancels all existing strategy orders and recreates a new bracket using the
     * fixed
     * initial SL/TP prices and the new quantity.
     * Supports both long and short positions.
     * 
     * @param ctx    The order context.
     * @param newQty The updated position quantity.
     */
    private void updateBracketQuantities(OrderContext ctx, int newQty) {
        Instrument instr = ctx.getInstrument();
        if (instr == null)
            return;

        if (newQty == 0) {
            this.info("Position flat - resetting tracking");
            resetTracking();
            return;
        }

        int absNewQty = Math.abs(newQty);

        this.info("Updating bracket to qty " + absNewQty);

        ctx.cancelOrders();
        resetTracking();

        float slPrice = fixedSlPrice;
        float tpPrice = fixedTpPrice;

        OrderAction exitAction = (newQty > 0) ? OrderAction.SELL : OrderAction.BUY;

        try {
            Order newStop = ctx.createStopOrder(exitAction, TIF.GTC, absNewQty, slPrice);
            Order newTP = ctx.createLimitOrder(exitAction, TIF.GTC, absNewQty, tpPrice);

            ctx.submitOrders(newStop, newTP);

            stopOrderId = newStop.getOrderId();
            tpOrderId = newTP.getOrderId();
            lastKnownQty = newQty;

            this.info("New bracket created: SL @" + slPrice + ", TP @" + tpPrice + ", Qty=" + absNewQty +
                    " (" + (newQty > 0 ? "Long" : "Short") + ")");
        } catch (Exception e) {
            this.info("Bracket update failed: " + e.getMessage());
        }
    }
}
